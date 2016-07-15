package webby.core.system
import java.nio.file.Path

import webby.api._
import webby.commons.system.ConsoleColors
import webby.core.SBTLink

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.NonFatal

/**
  * represents an application that can be reloaded in Dev Mode
  */
class ReloadableAppProvider(sbtLink: SBTLink) extends ApplicationProvider {

  // Use plain Java call here in case of scala classloader mess
  {
    if (System.getProperty("webby.debug.classpath") == "true") {
      System.out.println("\n---- Current ClassLoader ----\n")
      System.out.println(this.getClass.getClassLoader)
      System.out.println("\n---- The where is Scala? test ----\n")
      System.out.println(this.getClass.getClassLoader.getResource("scala/Predef$.class"))
    }
  }

  override def profile: Profile = Profile.Dev

  lazy val path: Path = sbtLink.projectPath.toPath

  println(ConsoleColors.yellow("--- (Running the application from SBT, auto-reloading is enabled) ---"))
  println()

  var onReloadAfterStartApp = mutable.Buffer[Application => Any]()

  var lastState: Either[Throwable, Application] = Left(new WebbyException("Not initialized", "?"))

  def get: Either[Throwable, Application] = {
    synchronized {
      var startTime = System.currentTimeMillis()
      sbtLink.reload match {
        case t: Throwable => Left(t)

        case projectClassLoader: ClassLoader =>
          try {
            if (lastState.isRight) {
              println()
              println(ConsoleColors.yellow("--- (RELOAD) ---"))
              println()
            }
            val reloadable = this

            val oldClassLoader = App.maybeApp.map(_.classloader).orNull

            // First, stop the old application if it exists
            App.stop()

            // Check for memory leaking threads on oldClassLoader
            locally {
              val threadGroup: ThreadGroup = Thread.currentThread().getThreadGroup
              val threads = new Array[Thread](threadGroup.activeCount())
              val count = Thread.enumerate(threads)
              var i = 0
              while (i < count) {
                val thread = threads(i)
                if (thread.getContextClassLoader == oldClassLoader &&
                  !thread.getName.startsWith("DestroyJavaVM") && // skip default thread
                  !thread.getName.startsWith("globalEventExecutor") && // skip netty thread
                  !thread.getName.startsWith("rebel-weak-reaper") // skip jrebel6 thread
                ) {
                  println("Old classLoader thread: " + thread.getId + " " + thread + " " + thread.getState)
                }
                i += 1
              }
            }

            val newApp: Application = new DefaultApplication(reloadable.path, projectClassLoader, Profile.Dev, Some(new SourceMapper {
              def sourceOf(className: String, line: Option[Int]) = {
                Option(sbtLink.findSource(className, line.map(_.asInstanceOf[java.lang.Integer]).orNull)).flatMap {
                  case Array(file: java.io.File, null) => Some((file, None))
                  case Array(file: java.io.File, line: java.lang.Integer) => Some((file, Some(line)))
                  case _ => None
                }
              }
            })) with DevSettings {
              lazy val devSettings: Map[String, String] = sbtLink.settings.asScala.toMap
            }

            App.start(newApp)
            onReloadAfterStartApp.foreach(_ (newApp))

            startTime = System.currentTimeMillis() - startTime
            Logger.webby.info("Application started (" + newApp.profile + ") in " + startTime + " ms")

            lastState = Right(newApp)
          } catch {
            case e: WebbyException => lastState = Left(e)
            case NonFatal(e) => lastState = Left(UnexpectedException(unexpected = Some(e)))
            case e: LinkageError => lastState = Left(UnexpectedException(unexpected = Some(e)))
          }
          lastState

        case null => lastState
      }
    }
  }
}
