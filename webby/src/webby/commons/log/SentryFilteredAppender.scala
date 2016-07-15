package webby.commons.log

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.getsentry.raven.RavenFactory
import com.getsentry.raven.logback.SentryAppender

/**
  * Расширение стандартного [[SentryAppender]].
  * Автоматически регистрирует фабрику [[SentryRavenFactory]] и делает её дефолтной.
  * Также, отсеивает все логи ниже WARN, и специфичные WARN-логи при старте/остановке сервера.
  *
  * Requires sbt dependency
  * {{{
  *   deps += "javax.servlet" % "javax.servlet-api" % "3.1.0"
  *   deps += "com.getsentry.raven" % "raven-logback" % "7.3.0" exclude("com.google.guava", "guava")
  * }}}
  */
class SentryFilteredAppender extends SentryAppender {
  RavenFactory.registerFactory(SentryRavenFactory)
  setRavenFactory(SentryRavenFactory.getClass.getName)

  /**
    * Отфильтровать сообщения, которые просто уведомляют о старте и остановке сервера.
    * Они являются ворнингами для того, чтобы попасть в app-warn.log, как разделитель между ошибками,
    * а также они имеют другой цвет при выводе в консоли ошибок.
    * Но они не являются ворнингами в привычном смысле.
    */
  override def append(iLoggingEvent: ILoggingEvent): Unit = {
    // Логи уровня INFO пропускаем
    if (!iLoggingEvent.getLevel.isGreaterOrEqual(Level.WARN)) return

    if (iLoggingEvent.getLevel == Level.WARN && iLoggingEvent.getLoggerName == "webby") {
      iLoggingEvent.getMessage match {
        case "Starting server" => return
        case m if m.startsWith("---------") => return
        case _ =>
      }
    }
    super.append(iLoggingEvent)
  }
}
