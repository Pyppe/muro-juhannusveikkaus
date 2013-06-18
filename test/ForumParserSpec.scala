import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import services.ForumParser

class ForumParserSpec extends Specification {
  
  "ForumParser" should {
    
    "findGuesses" in {
      //println(ForumParser.posts.map(_.user.name).mkString(","))
      ForumParser.findGuesses().size should be_>(5)
    }

    
  }

}