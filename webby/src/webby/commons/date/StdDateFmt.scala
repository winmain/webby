package webby.commons.date

import java.text.ParsePosition
import java.time.{LocalDate, ZoneId}
import java.util.Date
import javax.annotation.concurrent.ThreadSafe

/**
 * Форматтер даты, поддерживающий русские месяцы
 *
 * @param fmt Формат даты, см [[java.text.SimpleDateFormat]]
 */
@ThreadSafe
class StdDateFmt(val fmt: String) {
  val dateFormat = new ThreadLocal[RussianDateFormat] {
    override def initialValue() = new RussianDateFormat(fmt)
  }

  def unapply(s: String): Option[Date] = {
    Option.apply(dateFormat.get().parse(s, new ParsePosition(0)))
  }

  def parse(s: String): Date = dateFormat.get().parse(s)

  def format(date: Date): String = dateFormat.get().format(date)

  def format(v: LocalDate, zoneId: ZoneId): String = format(StdDates.toDate(v, zoneId))
}
