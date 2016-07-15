package webby.api.libs.concurrent

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import webby.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


/**
  * Helper to access the application defined Akka Actor system.
  *
  * Это опциональный модуль.
  * Для его использования нужно добавить зависимость в проект:
  * {{{
  * deps += "com.typesafe.akka" %% "akka-actor" % "2.4.8"
  * deps += "com.typesafe.akka" %% "akka-slf4j" % "2.4.8"
  * }}}
  */
object Akka {

  /**
    * Retrieve the application Akka Actor system.
    *
    * Example:
    * {{{
    * val newActor = Akka.system.actorOf[Props[MyActor]]
    * }}}
    */
  def system(implicit app: Application) = {
    app.plugin[AkkaPlugin].map(_.actorSystem).getOrElse {
      sys.error("Akka plugin is not registered.")
    }
  }

  /**
    * Executes a block of code asynchronously in the application Akka Actor system.
    *
    * Example:
    * {{{
    * val promiseOfResult = Akka.future {
    *    intensiveComputing()
    * }
    * }}}
    */
  def future[T](body: => T)(implicit app: Application): Future[T] = {
    Future(body)(system.dispatcher)
  }

}

/**
  * Plugin managing the application Akka Actor System.
  *
  * Это опциональный модуль.
  * Для его использования нужно добавить зависимость в проект:
  * {{{
  * deps += "com.typesafe.akka" %% "akka-actor" % "2.4.8"
  * deps += "com.typesafe.akka" %% "akka-slf4j" % "2.4.8"
  * }}}
  */
class AkkaPlugin(app: Application) extends Plugin {

  private var actorSystemEnabled = false

  lazy val actorSystem: ActorSystem = {
    actorSystemEnabled = true
    val system = ActorSystem("application", app.configuration.underlying.getConfig("webby"), app.classloader)
    Logger.webby.info("Starting application default Akka system.")
    system
  }


  override def onPrepareToShutdown(): Unit = {
    if (actorSystemEnabled) {
      Logger.webby.info("Shutdown application default Akka system.")
      actorSystem.terminate()
    }
  }

  override def onStop() {
    if (actorSystemEnabled) {
      Await.result(actorSystem.whenTerminated, Duration(1, TimeUnit.MINUTES))
    }
  }
}
