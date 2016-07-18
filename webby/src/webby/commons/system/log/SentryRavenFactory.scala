package webby.commons.system.log

import java.util

import com.getsentry.raven.dsn.Dsn
import com.getsentry.raven.event.EventBuilder
import com.getsentry.raven.event.helper.EventBuilderHelper
import com.getsentry.raven.event.interfaces.{HttpInterface, UserInterface}
import com.getsentry.raven.{DefaultRavenFactory, Raven}
import org.slf4j.LoggerFactory
import webby.api.mvc.{Action, RequestHeader}

/**
  * Костыли и monkey-patching, чтобы пропихнуть больше красивых данных в лог для Sentry
  *
  * Requires sbt dependency
  * {{{
  *   deps += "javax.servlet" % "javax.servlet-api" % "3.1.0"
  *   deps += "com.getsentry.raven" % "raven-logback" % "7.3.0" exclude("com.google.guava", "guava")
  * }}}
  */
object SentryRavenFactory extends DefaultRavenFactory {
  val log = LoggerFactory.getLogger(getClass)

  private val lastRequestContext = new ThreadLocal[RequestContext]()
  private val lastUser = new ThreadLocal[UserInterface]()

  override def createRavenInstance(dsn: Dsn): Raven = {
    val raven: Raven = super.createRavenInstance(dsn)
    raven.addBuilderHelper(new EventBuilderHelper {
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
    raven
  }

  def setRequestContext(action: Action, request: RequestHeader, body: Array[Byte]) {
    lastRequestContext.set(new RequestContext(action, request, body))
  }

  def setUser(ipAddress: String) {
    lastUser.set(new UserInterface(null, null, ipAddress, null))
  }
  def setUser(ipAddress: String, id: String, email: String) {
    lastUser.set(new UserInterface(id, null, ipAddress, email))
  }

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


  // ------------------------------- Inner classes -------------------------------

  case class RequestContext(action: Action, request: RequestHeader, body: Array[Byte])
}
