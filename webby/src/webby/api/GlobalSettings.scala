package webby.api

import java.nio.file.Path

import webby.api.mvc._

/**
 * Defines an application’s global settings.
 *
 * To define your own global settings, just create a `Global` object in the `_root_` package.
 * {{{
 * object Global extends GlobalSettings {
 *
 *   override def onStart(app: Application) {
 *     Logger.info("Application is started!!!")
 *   }
 *
 * }
 * }}}
 */
abstract class GlobalSettings {

  import Results._

  /**
    * Initialize all objects here.
    * [[webby.commons.system.OverridableObject]] successors must be called here.
    */
  def initObjects(app: Application) {}

  /**
   * Called before the application starts.
   *
   * Resources managed by plugins, such as database connections, are likely not available at this point.
   */
  def beforeStart(app: Application) {}

  /**
   * Called once the application is started.
   */
  def onStart(app: Application) {}

  /**
   * Called before application shutdown.
   */
  def onPrepareToShutdown(app: Application) {}

  /**
   * Called on application stop.
   */
  def onStop(app: Application) {}

  def createPlugins(app: Application): Iterable[Plugin] = Iterable.empty

  /**
   * Additional configuration provided by the application.  This is invoked by the default implementation of
   * onConfigLoad, so if you override that, this won't be invoked.
   */
  def configuration: Configuration = Configuration.empty

  /**
   * Called just after configuration has been loaded, to give the application an opportunity to modify it.
   *
   * @param config the loaded configuration
   * @param path the application path
   * @param classloader The applications classloader
   * @param profile The mode the application is running in
   * @return The configuration that the application should use
   */
  def onLoadConfig(config: Configuration, path: Path, classloader: ClassLoader, profile: Profile): Configuration =
    config ++ configuration

  /**
   * Called Just before the action is used.
   *
   */
  def doFilter(a: Action): Action = a

  /**
   * Called when an HTTP request has been received.
   *
   * The default is to use the application router to find the appropriate action.
   *
   * @param request the HTTP request header (the body has not been parsed yet)
   * @return an action to handle this request - if no action is returned, a 404 not found result will be sent to client
   * @see onActionNotFound
   */
  def onRouteRequest(request: RequestHeader): Option[Handler]

  /**
   * Called when an exception occurred.
   *
   * The default is to send the framework default error page.
   *
   * @param request The HTTP request header
   * @param ex The exception
   * @return The result to send to the client
   */
  def onError(request: RequestHeader, ex: Throwable, webbyException: Option[WebbyException]): Result = {
    try {
      if (ex.isInstanceOf[NotImplementedError]) NotImplemented("Not implemented error")
      else onError500(request, ex, webbyException)
    } catch {
      case e: Throwable =>
        Logger.error("Error while rendering default error page", e)
        InternalServerError
    }
  }

  def onError500(request: RequestHeader, ex: Throwable, webbyException: Option[WebbyException]): Result =
    InternalServerError("TODO: internal server error")

  /**
   * Called when no action was found to serve a request.
   *
   * The default is to send the framework default 404 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onHandlerNotFound(request: RequestHeader): Result =
    NotFoundRaw("TODO: not found default page")

  def onHandlerNotFoundAction = new Action {
    override def apply(header: RequestHeader, body: Array[Byte]): Result = onHandlerNotFound(header)
  }

  /**
   * Called when an action has been found, but the request parsing has failed.
   *
   * The default is to send the framework default 400 page.
   *
   * @param request the HTTP request header
   * @return the result to send to the client
   */
  def onBadRequest(request: RequestHeader, error: String): Result =
    BadRequest("TODO: bad request default page")

  def onRequestCompletion(request: RequestHeader, result: Result): Result = result

  /**
   * Приложение может предоставить дополнительные данные для логирования ошибки, разобрав request.
   * Например, можно получить id сессии, id юзера.
   */
  def getRequestInfoForLog(request: RequestHeader): Option[String] = None
}

/**
 * The Global plugin executes application's `globalSettings` `onStart` and `onStop`.
 */
class GlobalPlugin(app: Application) extends Plugin {

  // Call before start now
  app.global.beforeStart(app)

  /**
   * Called when the application starts.
   */
  override def onStart() {
    app.global.onStart(app)
  }

  /**
   * Called when the application stops.
   */
  override def onStop() {
    app.global.onStop(app)
  }

}
