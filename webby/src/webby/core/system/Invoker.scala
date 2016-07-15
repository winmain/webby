package webby.core

import akka.actor._
import com.typesafe.config._
import webby.api.{App, Logger}

/**
 * provides Webby's internal actor system and the corresponding actor instances
 */
private[webby] object Invoker {

  private def loadActorConfig = {
    val config = App.maybeApp.map(_.configuration.underlying).getOrElse {
      Logger.webby.warn("No application found at invoker init")
      ConfigFactory.load()
    }
    config.getConfig("webby")
  }

  val system: ActorSystem = ActorSystem("webby", loadActorConfig)

  val executionContext: scala.concurrent.ExecutionContext = system.dispatcher

}
