package controllers

import play.api._
import play.api.mvc._
import services.ForumParser

import org.json4s.{CustomSerializer, DefaultFormats, Formats, Extraction}
import org.json4s.ext.DateTimeSerializer
import org.json4s.jackson.JsonMethods.compact

object Application extends Controller {

  private implicit val jsonFormats: Formats = new DefaultFormats {
    override def dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
  } + DateTimeSerializer
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def guesses = Action {
    time("Find guesses") {
      Ok(toJSON(ForumParser.findGuesses)).as("application/json")
    }
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
