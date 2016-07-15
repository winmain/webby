package webby.api
import java.util.concurrent.{ExecutorService, SynchronousQueue, ThreadPoolExecutor, TimeUnit}
import javax.annotation.Nullable

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import webby.commons.concurrent.{ForceQueuePolicy, Threads}

import scala.collection.mutable.ArrayBuffer
import scala.util.control.NonFatal

object App {
  @Nullable private[api] var _app: Application = _

  def app: Application = {
    if (_app == null) sys.error("There is no started application")
    _app
  }

  @Nullable def appOrNull: Application = _app
  def maybeApp: Option[Application] = Option(_app)

  def profile: Profile = app.profile

  def isDev: Boolean = profile.isDev
  def isJenkins: Boolean = profile.isJenkins
  def isConsole: Boolean = profile.isConsole
  def isProd: Boolean = profile.isProd
  def isTest: Boolean = profile.isTest

  def isDevOrTest: Boolean = profile.isDevOrTest
  def isDevOrJenkins: Boolean = profile.isDevOrJenkins
  def isDevOrJenkinsOrTest: Boolean = profile.isDevOrJenkinsOrTest
  def isDevOrConsole: Boolean = profile.isDevOrConsole
  def isProdOrConsole: Boolean = profile.isProdOrConsole

  def noTest: Boolean = _app != null && !_app.profile.isTest

  /**
    * Starts this application.
    *
    * @param app the application to start
    */
  def start(app: Application) {
    // First stop previous app if exists
    stop()

    _app = app

    Threads.withContextClassLoader(app.classloader) {
      // --- for debug ---
      // app.plugins.foreach {plugin =>
      //   val t0 = System.currentTimeMillis()
      //   plugin.onStart()
      //   val t1 = System.currentTimeMillis()
      //   Logger.info("Started plugin " + plugin.getClass + " for " + (t1 - t0) + " ms")
      // }
      app.plugins.foreach(_.onStart())
    }

    app.profile match {
      case Profile.Test | Profile.Dev =>
      case mode => Logger.webby.info("Application started (" + mode + ")")
    }
  }

  private def oneThreadExecutor(threadGroupName: String): ThreadPoolExecutor =
    new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue[Runnable],
      new BasicThreadFactory.Builder().namingPattern(threadGroupName).build(), new ForceQueuePolicy)

  private def runExecutorWithContext(executor: ExecutorService)(fn: => Any): Unit =
    executor.execute(new Runnable {
      override def run(): Unit = Threads.withContextClassLoader(_app.classloader)(fn)
    })

  /**
    * Prepare to shutdown application (stop background processes) before shutting down Netty workers.
    */
  def prepareToShutdown(): Unit = {
    if (_app != null) {
      val executor = oneThreadExecutor("App.prepareToShutdown")
      _app.plugins.reverse.foreach {p =>
        runExecutorWithContext(executor) {
          stopPluginWrapper(p, _.onPrepareToShutdown(), "Preparing to shutdown plugin", "Error preparing to shutdown plugin")
        }
      }
      runExecutorWithContext(executor)(_app.global.onPrepareToShutdown(_app))
      executor.shutdown()
      executor.awaitTermination(5, TimeUnit.MINUTES)
    }
  }

  /**
    * Stops the current application.
    */
  def stop() {
    val executor = oneThreadExecutor("App.stop")
    if (_app != null) {
      _app.plugins.reverse.foreach {p =>
        runExecutorWithContext(executor) {
          stopPluginWrapper(p, _.onStop(), "Stopping plugin", "Error preparing to shutdown plugin")
        }
      }
    }
    onStopHandlers.foreach(handler => executor.execute(new Runnable {override def run(): Unit = handler()}))
    executor.shutdown()
    executor.awaitTermination(5, TimeUnit.MINUTES)
    _app = null
  }

  /**
    * Небольшой враппер, который следит за остановкой плагинов, и логгирует если что-то пошло не так.
    * Например, может возникнуть исключение, либо плагин долго останавливался.
    */
  private def stopPluginWrapper(p: Plugin, action: Plugin => Any, infoMessage: String, onErrorMessage: String): Unit = {
    _app.profile match {
      case Profile.Test => // no log message
      case Profile.Dev => Logger.webby.debug(infoMessage + ": " + p.getClass)
      case _ => Logger.webby.info(infoMessage + ": " + p.getClass)
    }

    val t0 = System.currentTimeMillis()
    try action(p) catch {case NonFatal(e) => Logger.webby.warn(onErrorMessage, e)}
    val time = System.currentTimeMillis() - t0

    if (time >= p.minTimeToLogInfoOnStop) {
      val msg = "Plugin " + p.getClass + " too slow on stop: " + time + " ms"
      if (time >= p.minTimeToLogWarnOnStop) Logger.webby.warn(msg) else Logger.webby.info(msg)
    }
  }


  private val onStopHandlers = ArrayBuffer[() => Any]()

  def addOnStopHandler(handler: => Any): Unit = {
    onStopHandlers += (() => handler)
  }
}
