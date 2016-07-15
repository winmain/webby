package webby.commons.text
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{LocalDate, LocalDateTime, ZonedDateTime}

import org.apache.commons.lang3.StringUtils

object DateFormats {
  val month: Array[String] = Array("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
  val monthR: Array[String] = Array("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
  val monthShort: Array[String] = Array("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

  /** Формат временной зоны +04:00 */
  private val zoneFormatter: DateTimeFormatter = new DateTimeFormatterBuilder().appendOffset("+HH:MM", "+00:00").toFormatter

  /** Формат: 18 июля 2012 */
  def dd_mmmm_yyyy(d: LocalDate): String =
    d.getDayOfMonth + " " + monthR(d.getMonthValue - 1) + " " + d.getYear

  /** Формат: 18 июля 2012 г. */
  def dd_mmmm_yyyy_y(d: LocalDate): String =
    d.getDayOfMonth + " " + monthR(d.getMonthValue - 1) + " " + d.getYear + " г."

  /** Формат: 18.07 */
  def dd_mm(d: LocalDate): String = d.getDayOfMonth + "." + zpad2(d.getMonthValue)

  /** Формат: 18 июл */
  def dd_mmm(d: LocalDate): String = d.getDayOfMonth + " " + monthShort(d.getMonthValue - 1)

  /** Формат: 18 июля */
  def dd_mmmm(d: LocalDate): String = d.getDayOfMonth + " " + monthR(d.getMonthValue - 1)

  /** Формат: июль 2012 */
  def mmmm_yyyy(d: LocalDate): String = month(d.getMonthValue - 1) + " " + d.getYear

  /** Формат: июль 2012 г. */
  def mmmm_yyyy_y(d: LocalDate): String = month(d.getMonthValue - 1) + " " + d.getYear + " г."

  /** Формат: июл 2012 */
  def mmm_yyyy(d: LocalDate): String = monthShort(d.getMonthValue - 1) + " " + d.getYear

  /** Формат: июл */
  def mmm(d: LocalDate): String = monthShort(d.getMonthValue - 1)

  /** Формат: июль */
  def mmmm(d: LocalDate): String = month(d.getMonthValue - 1)

  val mm_yyyy_formatter: DateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("MM.yyyy")
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1).toFormatter

  /** Формат: 18.07.2012 */
  def dd_mm_yyyy(d: LocalDate): String =
    d.getDayOfMonth + "." + zpad2(d.getMonthValue) + "." + d.getYear
  def dd_mm_yyyy(d: LocalDateTime): String = dd_mm_yyyy(d.toLocalDate)

  val dd_mm_yyyy_formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  /** Формат: 2012-07-18 */
  def yyyy_mm_dd(d: LocalDate): String =
    d.getYear + "-" + zpad2(d.getMonthValue) + "-" + zpad2(d.getDayOfMonth)

  /** Формат: 12-07-18 */
  def yy_mm_dd(d: LocalDate): String =
    zpad2(d.getYear % 100) + "-" + zpad2(d.getMonthValue) + "-" + zpad2(d.getDayOfMonth)

  /** Формат: 120718 */
  def yymmdd(d: LocalDate): String =
    zpad2(d.getYear % 100) + zpad2(d.getMonthValue) + zpad2(d.getDayOfMonth)

  /** Формат: 2012-07 */
  def yyyy_mm(d: LocalDate): String =
    d.getYear + "-" + zpad2(d.getMonthValue)

  /** Формат: 2012-07-18 13:55:02 */
  def yyyy_mm_dd_hh_mm_ss(d: LocalDateTime): String =
    d.getYear + "-" + zpad2(d.getMonthValue) + "-" + zpad2(d.getDayOfMonth) + " " +
      zpad2(d.getHour) + ":" + zpad2(d.getMinute) + ":" + zpad2(d.getSecond)

  /** Формат: 2012-07-18 13:55:02 */
  def yyyy_mm_dd_minor_hh_mm_ss_html(d: LocalDateTime): String =
    d.getYear + "-" + zpad2(d.getMonthValue) + "-" + zpad2(d.getDayOfMonth) + " <span class=\"minor\">" +
      zpad2(d.getHour) + ":" + zpad2(d.getMinute) + ":" + zpad2(d.getSecond) + "</span>"

  /** Формат: 2012-07-18T13:55:02 */
  def yyyy_mm_dd_T_hh_mm_ss(d: LocalDateTime): String =
    d.getYear + "-" + zpad2(d.getMonthValue) + "-" + zpad2(d.getDayOfMonth) + "T" +
      zpad2(d.getHour) + ":" + zpad2(d.getMinute) + ":" + zpad2(d.getSecond)

  /** Формат: 2012-07-18T13:55:02+04:00 (W3C) */
  def yyyy_mm_dd_T_hh_mm_ss_tz(d: ZonedDateTime): String =
    d.getYear + "-" + zpad2(d.getMonthValue) + "-" + zpad2(d.getDayOfMonth) + "T" +
      zpad2(d.getHour) + ":" + zpad2(d.getMinute) + ":" + zpad2(d.getSecond) + zoneFormatter.format(d)

  // ------------------------------- Private & protected methods -------------------------------

  private def zpad(value: Int, zeroes: Int): String =
    StringUtils.leftPad(String.valueOf(value), zeroes, '0')

  private def zpad2(value: Int) = zpad(value, 2)
}
