package controllers

import play.api._
import play.api.mvc._
import services.ForumParser
import play.api.cache.Cache

import org.json4s.{CustomSerializer, DefaultFormats, Formats, Extraction}
import org.json4s.ext.DateTimeSerializer
import org.json4s.jackson.JsonMethods.compact

object Application extends Controller {

  import play.api.Play.current

  private implicit val jsonFormats: Formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
  } + DateTimeSerializer
  
  def index = Action {
    Ok(views.html.index())
  }

  def guesses = Action {
    val guesses = Cache.getOrElse("guesses", 60*5) {
      time("Find guesses")(ForumParser.findGuesses)
    }

    Ok(toJSON(guesses)).as("application/json")
  }

  private def toJSON(obj: Any) = compact(Extraction.decompose(obj))

  def time[T](blockName: String) (block: => T): T = {
    val start = System.currentTimeMillis
    val value = block
    val end = System.currentTimeMillis
    Logger.info(s"$blockName took ${end - start} ms.")
    value
  }
  
}
