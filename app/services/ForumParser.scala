package services

import org.jsoup.nodes.Element
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
import scala.util.Try

case class Page(nextPage: Option[String], posts: Seq[Post])
case class Post(time: DateTime, user: String, url: String, guess: Guess)
case class Guess(land: Int, road: Int, water: Int)

case class FirstGuess(guess: Guess, user: String, url: String, time: DateTime, valid: Boolean, later: Seq[LateGuess])
case class LateGuess(user: String, url: String, delay: String)

case class CurrentStatus(text: String, land: Int, road: Int, water: Int)


object ForumParser {

  private val dtf = DateTimeFormat.forPattern("dd.MM.yyyy 'klo' HH:mm").withZone(DateTimeZone.forID("Europe/Helsinki"))
  private val Yesterday = """Eilen, (\d\d):(\d\d)""".r
  private val Today = """Tänään, (\d\d):(\d\d)""".r

  private val (forumUrl: String, validThreshold: DateTime) = {
    //"http://murobbs.plaza.fi/yleista-keskustelua/1014346-juhannusveikkaus-2013-a.html" -> dtf.parseDateTime("20.06.2013 klo 12:00")
    //"http://murobbs.muropaketti.com/threads/juhannusveikkaus-2014.1117759/" -> dtf.parseDateTime("19.06.2014 klo 12:00")
    //"http://murobbs.muropaketti.com/threads/juhannusveikkaus-2015.1221647/" -> dtf.parseDateTime("18.06.2015 klo 12:00")
    "http://murobbs.muropaketti.com/threads/juhannusveikkaus-2016.1313720/" -> dtf.parseDateTime("23.06.2016 klo 12:00")
  }

  private val durationFormatter = new PeriodFormatterBuilder()
    .appendDays().appendSuffix("pv")
    .appendSeparator(" ")
    .appendHours().appendSuffix("t")
    .appendSeparator(" ")
    .appendMinutes().appendSuffix("min")
    .toFormatter

  def findPosts() = findPostsFromUrl(forumUrl, Seq.empty)

  def findGuesses() = {

    val posts = findPosts().toList match {
      case head :: tail if head.user == "Kopernikus" || head.user == "janix" => Logger.info("drop one"); tail
      case xs => xs
    }

    posts.groupBy(_.guess).map { case (guess, posts) =>
      val firstPost = posts.head
      FirstGuess(guess, firstPost.user, firstPost.url, firstPost.time, firstPost.time.isBefore(validThreshold),
        posts.tail.map(p => LateGuess(p.user, p.url, duration(p.time, firstPost.time))))
    }
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
    }, 20.seconds)

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
    val posts = doc.select(".sectionMain.message").flatMap { post =>
      def selectUnique(selector: String) = post.select(selector).toList match {
        case el :: Nil => el
        case xs => throw new Exception(s"Too many <$selector>: $xs")
      }
      try {
        val messageEl = selectUnique(".messageText")
        val message = messageEl.select("> div").headOption.map { _ =>
          messageEl.ownText
        }.filter(_.trim.nonEmpty).getOrElse {
          messageEl.text
        }
        val postUrl = selectUnique(".messageMeta.otsikko a.datePermalink[href]").attr("abs:href")
        parseGuess(message, postUrl).flatMap { guess =>
          val time = parseTime(post.select(".DateTime").last)
          val user = selectUnique(".userText").text
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
    val nextPageUrl =
      doc.select(".PageNav a.text[href]").
      filter(_.text.toLowerCase.contains("seuraava")).
      lastOption.map(_.attr("abs:href"))

    Page(nextPageUrl, posts)
  }

  private def parseTime(el: Element): DateTime = {

    def date(minusDays: Int, hours: String, minutes: String) =
      DateTime.now.withTimeAtStartOfDay.minusDays(minusDays).withHourOfDay(hours.toInt).withMinuteOfHour(minutes.toInt)

    def textToDate(d: String) = d match {
      case Yesterday(hours, min) => date(1, hours, min)
      case Today(hours, min) => date(0, hours, min)
      case x => dtf.parseDateTime(x)
    }

    Try {
      new DateTime(el.attr("data-time").toLong * 1000)
    } getOrElse {
      textToDate(el.attr("title"))
    }

  }

  private val manualGuesses = Map(
    "5, 4, 6, 0" -> Guess(5,4,6),
    "1 1 3 0" -> Guess(1,1,3)
  )

  private def parseGuess(message: String, postUrl: String): Option[Guess] = {
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