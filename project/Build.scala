import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "muro-juhannusveikkaus"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "org.jsoup"   %  "jsoup"          % "1.7.2",
    "joda-time"   %  "joda-time"      % "2.2",
    "org.json4s"  %% "json4s-jackson" % "3.2.4",
    "org.json4s"  %% "json4s-ext"     % "3.2.4"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
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
