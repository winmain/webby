package webby.api

import java.nio.file.Path

import io.netty.handler.codec.http.HttpHeaderNames
import webby.api.mvc._
import webby.commons.concurrent.Threads
import webby.commons.log.SentryRavenFactory
import webby.commons.text.SB
import webby.core.system.SourceMapper

import scala.reflect.ClassTag

/**
  * A Webby application.
  *
  * Application creation is handled by the framework engine.
  *
  * This will create an application using the current classloader.
  */
abstract class Application {

  // ------------------------------- Reading configuration & getting global object  -------------------------------

  protected val initialConfiguration = Threads.withContextClassLoader(classloader)(Configuration.load(path))

  // ------------------------------- -------------------------------

  private val scalaGlobal: GlobalSettings = try {
    classloader.loadClass("Global$").getDeclaredField("MODULE$").get(null).asInstanceOf[GlobalSettings]
  } catch {
    case e: Exception => throw initialConfiguration.reportError("application.global", "Cannot initialize the Global object (%s)", Some(e))
  }

  /**
    * The global settings object used by this application.
    *
    * @see webby.api.GlobalSettings
    */
  val global: GlobalSettings = Threads.withContextClassLoader(classloader)(scalaGlobal)

  val configuration: Configuration = global.onLoadConfig(initialConfiguration, path, classloader, profile)

  // ------------------------------- Class fields -------------------------------

  /**
    * The absolute path hosting this application, mainly used by the `getFile(path)` helper method
    */
  def path: Path

  /**
    * The application's classloader
    */
  def classloader: ClassLoader

  /**
    * The `SourceMapper` used to retrieve source code displayed in error pages
    */
  def sources: Option[SourceMapper]

  /**
    * `Local`, `Jenkins`, `Console`, `Prod` or `Test`
    */
  def profile: Profile

  lazy val plugins: Vector[Plugin] = createPlugins

  def allowPlugins: Boolean = true

  protected def createPlugins: Vector[Plugin] = Threads.withContextClassLoader(classloader) {
    if (allowPlugins)
      try {
        val builder = Vector.newBuilder[Plugin]
        global.createPlugins(this).foreach(plugin => if (plugin.enabled) builder += plugin)
        builder.result()
      } catch {
        case e: Throwable => throw new WebbyException("Cannot load plugin", "", e)
      }
    else Vector.empty
  }


  /**
    * Retrieves a plugin of type `T`.
    *
    * For example, retrieving the DBPlugin instance:
    * {{{
    * val dbPlugin = application.plugin(classOf[DBPlugin])
    * }}}
    *
    * @tparam T the plugin type
    * @param  pluginClass the plugin’s class
    * @return the plugin instance, wrapped in an option, used by this application
    * @throws Error if no plugins of type `T` are loaded by this application
    */
  def plugin[T](pluginClass: Class[T]): Option[T] =
    plugins.find(p => pluginClass.isAssignableFrom(p.getClass)).map(_.asInstanceOf[T])

  /**
    * Retrieves a plugin of type `T`.
    *
    * For example, to retrieve the DBPlugin instance:
    * {{{
    * val dbPlugin = application.plugin[DBPlugin].map(_.api).getOrElse(sys.error("problem with the plugin"))
    * }}}
    *
    * @tparam T the plugin type
    * @return The plugin instance used by this application.
    * @throws Error if no plugins of type T are loaded by this application.
    */
  def plugin[T](implicit ct: ClassTag[T]): Option[T] = plugin(ct.runtimeClass).asInstanceOf[Option[T]]


  // Reconfigure logger
  {

    val validValues = Set("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF", "INHERITED")
    val setLevel = (level: String) =>
      level match {
        case "INHERITED" => null
        case lvl => ch.qos.logback.classic.Level.toLevel(lvl)
      }

    Logger.configure(
      Map("application.home" -> path.toAbsolutePath.toString),
      configuration.getConfig("logger").map {loggerConfig =>
        loggerConfig.keys.map {
          case "resource" | "file" | "url" => "" -> null
          case key@"root" => "ROOT" -> loggerConfig.getString(key, Some(validValues)).map(setLevel).get
          case key => key -> loggerConfig.getString(key, Some(validValues)).map(setLevel).get
        }.toMap
      }.getOrElse(Map.empty),
      profile)

  }

  private[webby] def handleAction(action: Action, request: RequestHeader, body: Array[Byte]): Result = {
    def logError(e: Throwable, fromResultException: Boolean = false) {
      SentryRavenFactory.setRequestContext(action, request, body)
      Logger.error(new SB {
        +e.getClass.getSimpleName + ": " + e.getMessage + "\n"
        +"Error for " + request.method.name() + " " + request.domain + request.uri
        if (fromResultException) +" (ResultException returned OK result, so client is happy)"
        request.headers.get(HttpHeaderNames.REFERER.toString).foreach(+"\nReferer: " + _)
        if (body.length > 0 && body.length < 100000) +"\nBody: " + new String(body)
        try global.getRequestInfoForLog(request).foreach(+"\nInfo: " + _)
        catch {
          case t: Throwable => +"\nInfo: " + t.toString
        }
      }.toString, e)
    }

    try {
      val result: Result = action(request, body)
      global.onRequestCompletion(request, result)
    } catch {
      case e: UsefulException => throw e
      case e: ResultException =>
        if (e.getCause != null) logError(e.getCause, fromResultException = true)
        e.getResult(request)

      case e: OutOfMemoryError =>
        logError(e)
        if (profile.isProd) global.onError(request, e, None)
        else sys.exit(2)

      case e: NoClassDefFoundError =>
        if (profile.isDev) {
          // Вероятнее всего, эту ошибку выдал JRebel при неудачной попытке заменить класс,
          // поэтому лучше выйти из приложения с кодом 2.
          // Sbt-web-runner должен перезагрузить приложение.
          sys.exit(2)
        } else {
          logError(e)
          global.onError(request, e, None)
        }

      case e: Throwable =>
        logError(e)
        global.onError(request, e, None)
    }
  }
}


class DefaultApplication(override val path: Path,
                         override val classloader: ClassLoader,
                         override val profile: Profile,
                         override val sources: Option[SourceMapper] = None,
                         override val allowPlugins: Boolean = true)
  extends Application
