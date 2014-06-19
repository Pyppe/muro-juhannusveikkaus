package services

import play.api.libs.ws._
import play.api.Play.current
import play.api.Logger

import org.jsoup.Jsoup
import org.joda.time._
import org.joda.time.format.{PeriodFormatterBuilder, DateTimeFormat}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.concurrent.duration._

case class Page(nextPage: Option[String], posts: Seq[Post])
case class Post(time: DateTime, user: String, url: String, guess: Guess)
case class Guess(land: Int, road: Int, water: Int)

case class FirstGuess(guess: Guess, user: String, url: String, time: DateTime, valid: Boolean, later: Seq[LateGuess])
case class LateGuess(user: String, url: String, delay: String)

case class CurrentStatus(text: String, land: Int, road: Int, water: Int)


object ForumParser {

  //private val forumUrl = "http://murobbs.plaza.fi/yleista-keskustelua/1014346-juhannusveikkaus-2013-a.html"
  private val forumUrl = "http://murobbs.plaza.fi/yleista-keskustelua/1117759-juhannusveikkaus-2014-a.html"
  private val dtf = DateTimeFormat.forPattern("dd.MM.yy, HH:mm").withZone(DateTimeZone.forID("Europe/Helsinki"))
  private val Yesterday = """Eilen, (\d\d):(\d\d)""".r
  private val Today = """Tänään, (\d\d):(\d\d)""".r

  //private val validThreshold = dtf.parseDateTime("20.06.13, 12:00")
  private val validThreshold = dtf.parseDateTime("19.06.14, 12:00")
  private def instructionFilter(post: Post) = post.url == "http://murobbs.plaza.fi/1711131824-post1.html"

  private val durationFormatter = new PeriodFormatterBuilder()
    .appendDays().appendSuffix("pv")
    .appendSeparator(" ")
    .appendHours().appendSuffix("t")
    .appendSeparator(" ")
    .appendMinutes().appendSuffix("min")
    .toFormatter
    
  def findPosts() = findPostsFromUrl(forumUrl, Seq.empty)

  def findGuesses() =
    findPosts().filterNot(instructionFilter).groupBy(_.guess).map { case (guess, posts) =>
      val firstPost = posts.head
      FirstGuess(guess, firstPost.user, firstPost.url, firstPost.time, firstPost.time.isBefore(validThreshold),
        posts.tail.map(p => LateGuess(p.user, p.url, duration(p.time, firstPost.time))))
    }

  def currentStatus(): Option[CurrentStatus] =
    Await.result(WS.url(forumUrl).get.map { response =>
      val doc = Jsoup.parse(response.body)
      try {
        val message = doc.select("#posts .page [id^=post_message_]").head.text
        """TILANNE [^*]+\*""".r.findFirstMatchIn(message).flatMap { m =>
          val statusLine = m.group(0).replace("*", "").replace("'","").replace("\"","").trim
          """M(\d+).*T(\d+).*V(\d+)""".r.findFirstMatchIn(statusLine).map { m =>
            CurrentStatus(statusLine, m.group(1).toInt, m.group(2).toInt, m.group(3).toInt)
          }
        }
      } catch {
        case e: Exception => None
      }
    }, 10 seconds)
  
  private def findPostsFromUrl(url: String, posts: Seq[Post]): Seq[Post] = {
    val page = Await.result(WS.url(url).get.map(parsePage), 10 seconds)
    page.nextPage match {
      case Some(url) => findPostsFromUrl(url, posts ++ page.posts)
      case None => posts ++ page.posts
    }
  }

  private val SkippedPosts = Set("http://murobbs.plaza.fi/1713198286-post1.html")
  private def parsePage(response: WSResponse) = {
    val doc = Jsoup.parse(response.body)
    val posts = doc.select("#posts .page").flatMap { post =>
      try {
        val messageEl = post.select("[id^=post_message_]").head
        val message = messageEl.select("> div").headOption.map { _ =>
          messageEl.ownText
        }.getOrElse {
          messageEl.text
        }
        parseGuess(message).flatMap { guess =>
          val time = parseTime(post.select(".thead").head.text)
          val user = post.select(".bigusername").head.text
          val postUrl = post.select("[id^=postcount]").head.attr("href")
          Option(postUrl).filterNot(SkippedPosts).map { postUrl =>
            Post(time, user, postUrl, guess)
          }
        }
      } catch {
        case e: Exception =>
          Logger.error("Error parsing post", e)
          None
      }
    }
    val nextPageUrl = doc.select(".pagenav:eq(0) .alt2 + .alt1").select("a").headOption.map(_.attr("href"))
    Page(nextPageUrl, posts)
  }

  private def parseTime(d: String) = {

    def date(minusDays: Int, hours: String, minutes: String) =
      DateTime.now.withTimeAtStartOfDay.minusDays(minusDays).withHourOfDay(hours.toInt).withMinuteOfHour(minutes.toInt)

    d match {
      case Yesterday(hours, min) => date(1, hours, min)
      case Today(hours, min) => date(0, hours, min)
      case x => dtf.parseDateTime(x)
    }
  }

  private val manualGuesses = Map(
    "Ma-Ti-Ve Kolme-Kolme-Kolme" -> Guess(3,3,3),
    "M:3 5:7 V:13" -> Guess(3,7,13)
  )

  private def parseGuess(message: String): Option[Guess] = {
    def findMatch(key: String): Option[Int] = {
      val pattern = """%s%s *[-=:]? *(\d+)"""
      val firstMatch = pattern.format(key, "").r.findFirstMatchIn(message).orElse {
        pattern.format(key, "[a-zäö]*").r.findFirstMatchIn(message)
      }
      firstMatch.map(_.group(1).toInt)
    }

    if ("""\?(\d+)-(\d+)-(\d+)""".r.findFirstMatchIn(message).isDefined) {
      None // skip urls
    } else {
      (findMatch("[mM]"), findMatch("[tT]"), findMatch("[vV]")) match {
        case (Some(m), Some(t), Some(v)) =>
          Some(Guess(m, t, v))
        case _ =>
          // Let's catch the couple of odd entries
          """(\d+)-(\d+)-(\d+)""".r.findFirstMatchIn(message).map { m =>
            Some(Guess(m.group(1).toInt, m.group(2).toInt, m.group(3).toInt))
          }.getOrElse {
            manualGuesses.get(message).orElse {
              Logger.warn(s"Could not determine guess from $message")
              None
            }
          }
      }
    }
  }

  def humanizeMillis(millis: Long): String =
    durationFormatter.print(new Period(0L, millis, PeriodType.dayTime))

  def duration(d1: DateTime, d2: DateTime) = humanizeMillis(math.abs(d1.getMillis - d2.getMillis))

}