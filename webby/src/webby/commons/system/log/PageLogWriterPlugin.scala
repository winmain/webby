package webby.commons.system.log

import java.io.File

import webby.api.{Application, Plugin}
import webby.mvc.AppPluginHolder
import webby.commons.text.SB

import scala.reflect.ClassTag

/**
 * Абстрактный класс для подключения всех сторонних логгеров, которые поддерживают переоткрытие файла
 * на сигнал USR2, а также умеют стартовать и корректно завершаться вместе с приложением.
 */
abstract class LogWriterPlugin(app: Application) extends Plugin {
  val fileName: Option[String]
  def bufferSize: Int = 8192

  lazy val writer: SignalRollingSyncLogWriter = fileName match {
    case Some(f) => new SignalRollingSyncLogWriter(new File(f), bufferSize)
    case None => null
  }

  /**
   * Is the plugin enabled?
   */
  override def enabled: Boolean = fileName.isDefined

  /**
   * Called when the application stops.
   */
  override def onStop() {
    if (writer != null) writer.closeWriter()
  }
}

/**
 * Удобный холдер для работы с наследниками класса LogWriterPlugin
 */
class LogWriterHolder[T <: LogWriterPlugin](implicit ct: ClassTag[T]) extends AppPluginHolder[T] {
  override def onDisabledPlugin: T = null.asInstanceOf[T]

  def writeLn(s: String) {
    val plugin: T = get
    if (plugin != null) plugin.writer.writeLn(s)
  }
  def writeLn(sb: SB): Unit = {
    val plugin: T = get
    if (plugin != null) plugin.writer.writeLn(sb.str)
  }
  def write(sb: SB): Unit = {
    val plugin: T = get
    if (plugin != null) plugin.writer.write(sb.str)
  }
}


/**
 * Логгер обработки страниц page.log
 * Каждый запрос к серверу логируется сюда
 */
class PageLogWriterPlugin(app: Application) extends LogWriterPlugin(app) {
  override val fileName: Option[String] = app.configuration.getString("pagelog.file").filter(!_.isEmpty)
  override def bufferSize: Int = app.configuration.getInt("pagelog.buffer").getOrElse(32768)
}

/**
 * Логгер отправленных писем mail.log
 */
class MailLogWriterPlugin(app: Application) extends LogWriterPlugin(app) {
  override val fileName: Option[String] = app.configuration.getString("maillog.file").filter(!_.isEmpty)
}

/**
 * Логгер обработки платежей pay.log
 */
class PayLogWriterPlugin(app: Application) extends LogWriterPlugin(app) {
  override val fileName: Option[String] = app.configuration.getString("paylog.file").filter(!_.isEmpty)
  override def bufferSize: Int = 0
}

/**
 * Логгер парсинга партнёрских вакансий partner.log
 */
class PartnerLogWriterPlugin(app: Application) extends LogWriterPlugin(app) {
  override val fileName: Option[String] = app.configuration.getString("partnerlog.file").filter(!_.isEmpty)
}
