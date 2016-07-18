package webby.commons.system.cron

import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.LocalDateTime

import webby.commons.system.OverridableObject
import webby.commons.text.DateFormats

// ------------------------------- CronLog -------------------------------

trait CronLog {
  def append(started: Boolean)
  def start(): this.type = {append(started = true); this}
  def finish(): Unit = append(started = false)
}

/**
  * Класс, умеющий делать записи крон-заданий (или просто длинных действий) в Paths.cronlog.
  *
  * @param logPath  Путь до файла, в который пишется лог
  * @param fullName Полное имя задания, включая префикс "app:" или "longAction:"
  */
class CommonCronLog(logPath: Path, fullName: String) extends CronLog {
  override def append(started: Boolean) {
    val stream = Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    val dateStr = DateFormats.yyyy_mm_dd_T_hh_mm_ss(LocalDateTime.now())
    val sign = if (started) "+" else "-"
    stream.write((dateStr + " " + sign + fullName + "\n").getBytes)
    stream.close()
  }
}

object StubCronLog extends CronLog {
  override def append(started: Boolean): Unit = {}
}

// ------------------------------- CronLogFactory -------------------------------

/**
  * Для правильного использования этой фабрики нужно задать объект:
  * {{{
  *   object CronLogs extends CronLogFactory.Common(Paths.cronlog)
  * }}}
  */
object CronLogFactory extends OverridableObject {
  abstract class Value extends Base {
    def create(fullName: String): CronLog

    def forQuartz(group: String, name: String) = create("app:" + group + ":" + name)
    def forLongAction(name: String): CronLog = create("longAction:" + name)
  }

  class Stub extends Value {
    override def create(fullName: String): CronLog = StubCronLog
  }

  class Common(logPath: Path) extends Value {
    override def create(fullName: String): CronLog = new CommonCronLog(logPath, fullName)
  }

  override protected def default: Value = new Stub
}
