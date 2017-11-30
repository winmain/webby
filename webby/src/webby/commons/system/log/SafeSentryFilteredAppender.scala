package webby.commons.system.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import io.sentry.environment.SentryEnvironment
import io.sentry.event.EventBuilder
import io.sentry.logback.SentryAppender

/**
  * Extension for [[io.sentry.logback.SentryAppender]].
  * The main goal for this appender is to forward log events to [[SafeSentryThread]]
  * so Sentry code will be run in isolated thread.
  *
  * Usage:
  * In logback config `logger.xml`:
  * {{{
  *   <appender name="sentry" class="webby.commons.system.log.SafeSentryFilteredAppender" />
  *
  *   <root level="WARN">
  *     <appender-ref ref="sentry"/>
  *   </root>
  * }}}
  *
  * Also, you need to create `sentry.properties` file in resources:
  * {{{
  *   factory=webby.commons.system.log.SentryClientFactory
  *   dsn=https://login:pass@sentry.example.com</dsn>
  *   buffer.dir=sentry-events
  *   buffer.size=100
  *   buffer.flushtime=10000
  * }}}
  * (buffer settings are optional)
  *
  * Requires sbt dependency
  * {{{
  *   deps += "javax.servlet" % "javax.servlet-api" % "3.1.0"
  *   deps += "io.sentry" % "sentry-logback" % "1.6.3" % "optional"
  * }}}
  */
class SafeSentryFilteredAppender extends SentryAppender {

  private val thread = new SafeSentryThread()

  override def append(iLoggingEvent: ILoggingEvent): Unit = {
    // Do not log the event if the current thread is managed by sentry
    if (!isLoggable(iLoggingEvent) || SentryEnvironment.isManagingThread) return

    SentryEnvironment.startManagingThread()
    try {
      val eventBuilder: EventBuilder = createEventBuilder(iLoggingEvent)
      thread.enqueue(eventBuilder)
    } catch {
      case e: Exception =>
        addError("An exception occurred while creating a new event in Sentry", e)
    } finally SentryEnvironment.stopManagingThread()
  }

  /**
    * Отфильтровать сообщения, которые просто уведомляют о старте и остановке сервера.
    * Они являются ворнингами для того, чтобы попасть в app-warn.log, как разделитель между ошибками,
    * а также они имеют другой цвет при выводе в консоли ошибок.
    * Но они не являются ворнингами в привычном смысле.
    */
  def isLoggable(iLoggingEvent: ILoggingEvent): Boolean = {
    if (iLoggingEvent.getLevel == Level.WARN && iLoggingEvent.getLoggerName == "webby") {
      iLoggingEvent.getMessage match {
        case "Starting server" => false
        case m if m.startsWith("---------") => false
        case _ => true
      }
    } else true
  }

  // ------------------------------- Start/stop -------------------------------

  override def start(): Unit = {
    thread.start()
    super.start()
  }

  override def stop(): Unit = {
    thread.gracefullyStop()
    super.stop()
  }
}
