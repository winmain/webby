package webby.core.server

import java.net.InetSocketAddress
import java.nio.file.Files

import webby.api._
import webby.api.mvc.Results._
import webby.api.mvc._
import webby.core.system.ApplicationProvider

/**
 * provides generic server behaviour for Webby applications
 */
abstract class Server {
  // First delete the default log file for a fresh start (only in Dev Mode)
  if (profile.isDevOrJenkins) Files.deleteIfExists(applicationProvider.path.resolve("logs/application.log"))

  // Configure the logger for the first time
  Logger.configure(
    Map("application.home" -> applicationProvider.path.toAbsolutePath.toString),
    profile = profile)

  Logger.webby.warn("Starting server")

  def applicationProvider: ApplicationProvider

  def profile: Profile = applicationProvider.profile

  /**
    * Вернуть обработчик для заданного запроса #request вместе с актуальным application,
    * либо вернуть результат с ошибкой, если что-то пошло не так.
    *
    * Что может пойти не так:
    * * Приложение может быть проинициализировано с ошибкой (applicationProvider.get возвратил Left)
    * * Происходит необработанный exception во время получения обработчика запроса
    */
  def getHandlerFor(request: RequestHeader): Either[Result, (Handler, Application)] = {

    def logExceptionAndGetResult(e: Throwable): Result = {
      Logger.error(
        """
        |
        |! %sInternal server error, for (%s) [%s] ->
        |""".stripMargin.format(e match {
          case p: WebbyException => "@" + p.id + " - "
          case _ => ""
        }, request.method.name(), request.uri),
        e)

      e match {
        case _: NotImplementedError => NotImplemented("Not implemented error")
        case _ => InternalServerError("Internal server error")
      }
    }

    try {
      applicationProvider.get match {
        case Right(app) =>
          app.global.onRouteRequest(request) match {
            case Some(handler) => Right((handler, app))
            case None => Right((app.global.onHandlerNotFoundAction, app))
          }
        case Left(throwable) => throwable match {
          case e: WebbyException =>
            Logger.webby.error("Error initializing app", e)
            Left(Results.InternalServerError(e.toString + ", title:" + e.title + ", description:" + e.description))
          case t => Left(logExceptionAndGetResult(t))
        }
      }
    } catch {
      case e: ThreadDeath => throw e
      case e: VirtualMachineError => throw e
      case e: ResultException => Left(e.getResult(request))
      case e: Throwable => Left(logExceptionAndGetResult(e))
    }
  }

  def stop() {
    Logger.shutdown()
  }

}

/**
 * provides a stoppable Server
 */
trait ServerWithStop {
  def stop(): Unit

  def mainAddress: InetSocketAddress
}

