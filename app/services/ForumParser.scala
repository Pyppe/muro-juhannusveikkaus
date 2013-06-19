package services

import org.jsoup.Jsoup
import org.joda.time._
import org.joda.time.format.{PeriodFormatterBuilder, DateTimeFormat}

import play.api.libs.ws._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import play.api.Logger
import play.api.libs.ws.Response
import scala.Some

case class Page(nextPage: Option[String], posts: Seq[Post])
case class Post(time: DateTime, user: String, url: String, guess: Guess)
case class Guess(land: Int, road: Int, water: Int)

case class FirstGuess(guess: Guess, user: String, url: String, time: DateTime, later: Seq[LateGuess])
case class LateGuess(user: String, url: String, delay: String)


object ForumParser {

  private val forumUrl = "http://murobbs.plaza.fi/yleista-keskustelua/1014346-juhannusveikkaus-2013-a.html"
  private val dtf = DateTimeFormat.forPattern("dd.MM.yy, HH:mm").withZone(DateTimeZone.forID("Europe/Helsinki"))
  private val Yesterday = """Eilen, (\d\d):(\d\d)""".r
  private val Today = """Tänään, (\d\d):(\d\d)""".r

  private val durationFormatter = new PeriodFormatterBuilder()
    .appendDays().appendSuffix("pv")
    .appendSeparator(" ")
    .appendHours().appendSuffix("t")
    .appendSeparator(" ")
    .appendMinutes().appendSuffix("min")
    .toFormatter
    
  def findPosts() = findPostsFromUrl(forumUrl, Seq.empty)

  def findGuesses() = {
    findPosts().groupBy(_.guess).map { case (guess, posts) =>
      val firstPost = posts.head
      FirstGuess(guess, firstPost.user, firstPost.url, firstPost.time,
        posts.tail.map(p => LateGuess(p.user, p.url, duration(p.time, firstPost.time))))
    }
  }
  
  private def findPostsFromUrl(url: String, posts: Seq[Post]): Seq[Post] = {
    val page = Await.result(WS.url(url).get.map(parsePage), 10 seconds)
    page.nextPage match {
      case Some(url) => findPostsFromUrl(url, posts ++ page.posts)
      case None => posts ++ page.posts
    }
  }
  
  private def parsePage(response: Response) = {
    val doc = Jsoup.parse(response.body)
    val posts = doc.select("#posts .page").flatMap { post =>
      try {
        val message = post.select("[id^=post_message_]").head.ownText
        parseGuess(message).map { guess =>
          val time = parseTime(post.select(".thead").head.text)
          val user = post.select(".bigusername").head.text
          val postUrl = post.select("[id^=postcount]").head.attr("href")
          Post(time, user, postUrl, guess)
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
      new DateTime(new DateMidnight).minusDays(minusDays).withHourOfDay(hours.toInt).withMinuteOfHour(minutes.toInt)

    d match {
      case Yesterday(hours, min) => date(1, hours, min)
      case Today(hours, min) => date(0, hours, min)
      case x => dtf.parseDateTime(x)
    }
  }

  private def parseGuess(message: String): Option[Guess] = {
    def findMatch(key: String) =
      """%s *= *(\d+)""".format(key).r.findFirstMatchIn(message).map(_.group(1)).toSeq

    findMatch("[mM]") ++ findMatch("[tT]") ++ findMatch("[vV]") match {
      case seq: Seq[String] if seq.size == 3 => Some(Guess(seq(0).toInt, seq(1).toInt, seq(2).toInt))
      case _ => None
    }
  }

  def humanizeMillis(millis: Long): String =
    durationFormatter.print(new Period(0L, millis, PeriodType.dayTime))

  def duration(d1: DateTime, d2: DateTime) = humanizeMillis(math.abs(d1.getMillis - d2.getMillis))

}