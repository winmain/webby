package webby.core.system

import java.io._
import java.nio.file.Path

import webby.api._
import webby.api.mvc._


/**
  * provides source code to be displayed on error pages
  */
trait SourceMapper {

  def sourceOf(className: String, line: Option[Int] = None): Option[(File, Option[Int])]

  def sourceFor(e: Throwable): Option[(File, Option[Int])] = {
    e.getStackTrace.find(element => sourceOf(element.getClassName).isDefined).flatMap {interestingStackTrace =>
      sourceOf(interestingStackTrace.getClassName, Option(interestingStackTrace.getLineNumber))
    }
  }

}

trait DevSettings {
  def devSettings: Map[String, String]
}

/**
  * generic layout for initialized Applications
  */
trait ApplicationProvider {
  def profile: Profile
  def path: Path
  def get: Either[Throwable, Application]
}

trait HandleWebCommandSupport {
  def handleWebCommand(request: webby.api.mvc.RequestHeader, sbtLink: webby.core.SBTLink, path: java.io.File): Option[Result]
}

/**
  * creates and initializes an Application
  * @param applicationPath location of an Application
  * @param profile         Application profile
  * @param allowPlugins    allow plugins for Application instance
  */
class StaticAppProvider(applicationPath: Path, val profile: Profile, allowPlugins: Boolean = true) extends ApplicationProvider {

  val application = new DefaultApplication(applicationPath, Thread.currentThread().getContextClassLoader, profile, allowPlugins = allowPlugins)

  App.start(application)

  override def path: Path = applicationPath
  override def get = Right(application)
}

/**
  * wraps and starts a fake application (used in tests)
  * @param application fake Application
  */
class TestAppProvider(application: Application) extends ApplicationProvider {

  App.start(application)

  override def profile: Profile = Profile.Test
  override def path: Path = application.path
  override def get = Right(application)
}
