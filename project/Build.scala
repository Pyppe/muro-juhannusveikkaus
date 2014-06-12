import sbt._
import Keys._
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

  val appName         = "muro-juhannusveikkaus"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    "org.jsoup"   %  "jsoup"          % "1.7.2",
    "joda-time"   %  "joda-time"      % "2.2",
    "org.json4s"  %% "json4s-jackson" % "3.2.10",
    "org.json4s"  %% "json4s-ext"     % "3.2.10"
  )


  val main = Project(appName, file(".")).enablePlugins(play.PlayScala).settings(
    version := appVersion,
    libraryDependencies ++= appDependencies ++ Seq(cache, ws),
    scalaVersion := "2.11.1"
  )
  
  /*
$('#posts .page').each(function () {
  var $page = $(this);
  var createTime = $.trim($page.find('.thead:first').text());
  var username = $page.find('.bigusername').text();
  var userProfile = $page.find('.bigusername').attr('href');
  var message = $page.find('[id^="post_message_"]').text();
  var post = {
    createTime: createTime,
    username: username,
    userProfile: userProfile,
    message: message
  };
  console.log(post);
});
});

   */

}
