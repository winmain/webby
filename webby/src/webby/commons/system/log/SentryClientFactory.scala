package webby.commons.system.log

import io.sentry.dsn.Dsn
import io.sentry.event.EventBuilder
import io.sentry.event.helper.EventBuilderHelper
import io.sentry.event.interfaces.{HttpInterface, UserInterface}
import io.sentry.{DefaultSentryClientFactory, SentryClient}
import webby.api.mvc.{Action, RequestHeader}

/**
  * Костыли и monkey-patching, чтобы пропихнуть больше красивых данных в лог для Sentry.
  *
  * Usage:
  * Create file named `sentry.properties` containing:
  * {{{
  *   factory=webby.commons.system.log.SentryClientFactory
  * }}}
  *
  * Requires sbt dependency
  * {{{
  *   deps += "javax.servlet" % "javax.servlet-api" % "3.1.0"
  *   deps += "io.sentry" % "sentry-logback" % "1.6.3" % "optional"
  * }}}
  */
class SentryClientFactory extends DefaultSentryClientFactory {
  import SentryClientFactory._
  useSentry()

  override def createSentryClient(dsn: Dsn): SentryClient = {
    val client: SentryClient = super.createSentryClient(dsn)
    client.addBuilderHelper(new EventBuilderHelper {
      override def helpBuildingEvent(eventBuilder: EventBuilder): Unit = {
        val user: UserInterface = lastUser.get()
        if (user != null) {
          eventBuilder.withSentryInterface(user)
          lastUser.remove()
        }

        val requestContext: RequestContext = lastRequestContext.get()
        if (requestContext != null) {
          eventBuilder.withSentryInterface(new HttpInterface(new SentryHttpServletRequestAdapter(requestContext.request)))
          lastRequestContext.remove()
        }
      }
    })
    client
  }

  /*
  override def getNotInAppFrames: util.Collection[String] = util.Arrays.asList(
    "com.sun.",
    "java.",
    "javax.",
    "org.omg.",
    "sun.",
    "junit.",
    "com.intellij.rt.",
    "webby.api.Logger",
    "io.netty.util.internal.logging.",
    "sun.nio.fs."
  )
  */
}

object SentryClientFactory {
  private var usingSentry = false
  private val lastRequestContext = new ThreadLocal[RequestContext]()
  private val lastUser = new ThreadLocal[UserInterface]()

  def useSentry(): Unit = {
    usingSentry = true
  }

  def setRequestContext(action: Action, request: RequestHeader, body: Array[Byte]) {
    if (usingSentry) lastRequestContext.set(new RequestContext(action, request, body))
  }

  def setUser(ipAddress: String) {
    if (usingSentry) lastUser.set(new UserInterface(null, null, ipAddress, null))
  }
  def setUser(ipAddress: String, id: String, email: String) {
    if (usingSentry) lastUser.set(new UserInterface(id, null, ipAddress, email))
  }

  // ------------------------------- Inner classes -------------------------------

  case class RequestContext(action: Action, request: RequestHeader, body: Array[Byte])
}
