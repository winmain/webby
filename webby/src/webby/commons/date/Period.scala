package webby.commons.date

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Класс для хранения периода двух дат.
 *
 * @param fromIncl Дата от, включительно
 * @param toExcl Дата до, исключительно
 */
case class Period(fromIncl: LocalDate, toExcl: LocalDate) {
  require(!fromIncl.isAfter(toExcl), "Invalid period. Date fromIncl " + fromIncl + " must be equal or earlier than toExcl " + toExcl)

  def contains(date: LocalDate): Boolean = !date.isBefore(fromIncl) && date.isBefore(toExcl)
  def contains(other: Period): Boolean = !other.fromIncl.isBefore(fromIncl) && !other.toExcl.isAfter(toExcl)
  def intersects(other: Period): Boolean = other.fromIncl.isBefore(toExcl) && other.toExcl.isAfter(fromIncl)
  def intersectOrAdjacent(other: Period): Boolean = !other.fromIncl.isAfter(toExcl) && !other.toExcl.isBefore(fromIncl)

  def daysLong: Long = ChronoUnit.DAYS.between(fromIncl, toExcl)
  def days: Int = daysLong.toInt

  /**
   * Попытаться склеить два периода, и вернуть результат.
   *
   * @return Если периоды пересекаются, то возвращаем новый общий Some(period), включающий в себя оба периода.
   *         Если же они не пересекаются, то возвращаем None
   */
  def tryGlue(other: Period): Option[Period] = {
    if (intersectOrAdjacent(other)) {
      val c1 = fromIncl.compareTo(other.fromIncl)
      val c2 = toExcl.compareTo(other.toExcl)
      if (c1 <= 0 && c2 >= 0) Some(this)
      else if (c1 > 0 && c2 < 0) Some(other)
      else Some(Period(if (c1 < 0) fromIncl else other.fromIncl, if (c2 > 0) toExcl else other.toExcl))
    } else None
  }
}

object Period extends Ordering[Period] {
  override def compare(x: Period, y: Period): Int =
    x.fromIncl.compareTo(y.fromIncl) match {
      case 0 => x.toExcl.compareTo(y.toExcl)
      case v => v
    }
}
