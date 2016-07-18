package webby.commons.time

import java.sql.Timestamp
import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, Temporal}
import java.util.Date

object StdDates {dates =>
  import scala.language.implicitConversions

  /** Number of milliseconds in a standard second. */
  val Second: Long = 1000
  /** Number of milliseconds in a standard minute. */
  val Minute: Long = 60 * Second
  /** Number of milliseconds in a standard hour. */
  val Hour: Long = 60 * Minute
  /** Number of milliseconds in a standard day. */
  val Day: Long = 24 * Hour
  val DayDouble: Double = Day

  val httpDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O")
  val httpDateFormatGmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
  def httpDateFormatMillis(millis: Long): String = httpDateFormatGmt.format(OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC))
  def httpDateFormatLDT(v: LocalDateTime): String = httpDateFormatGmt.format(v.atOffset(ZoneOffset.UTC))

  def daysBetween(t0: Temporal, t1: Temporal): Int = ChronoUnit.DAYS.between(t0, t1).toInt
  def monthsBetween(t0: Temporal, t1: Temporal): Int = ChronoUnit.MONTHS.between(t0, t1).toInt
  def yearsBetween(t0: Temporal, t1: Temporal): Int = ChronoUnit.YEARS.between(t0, t1).toInt

  def nowTimestamp: Timestamp = new Timestamp(System.currentTimeMillis())

  def toInstant(v: LocalDateTime, zoneId: ZoneId): Instant = v.atZone(zoneId).toInstant
  def toInstant(v: LocalDate, zoneId: ZoneId): Instant = v.atStartOfDay(zoneId).toInstant
  def toInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

  def toTimestamp(v: LocalDateTime, zoneId: ZoneId): Timestamp = Timestamp.from(toInstant(v, zoneId))
  def toTimestamp(v: LocalDate, zoneId: ZoneId): Timestamp = Timestamp.from(toInstant(v, zoneId))

  def toDate(v: LocalDateTime, zoneId: ZoneId): Date = Date.from(toInstant(v, zoneId))
  def toDate(v: LocalDate, zoneId: ZoneId): Date = Date.from(toInstant(v, zoneId))

  def toMillis(v: LocalDateTime, zoneId: ZoneId): Long = toInstant(v, zoneId).toEpochMilli
  def toMillis(v: LocalDate, zoneId: ZoneId): Long = toInstant(v, zoneId).toEpochMilli

  def toLocalDateTime(v: Date, zoneId: ZoneId): LocalDateTime = toLocalDateTime(v.getTime, zoneId)
  def toLocalDateTime(millis: Long, zoneId: ZoneId): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId)

  def toLocalDate(v: Date, zoneId: ZoneId): LocalDate = toLocalDate(v.getTime, zoneId)
  def toLocalDate(millis: Long, zoneId: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate

  def toOffsetDateTime(millis: Long, zoneId: ZoneId): OffsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId)

  def toZonedDateTime(v: LocalDateTime, zoneId: ZoneId): ZonedDateTime = ZonedDateTime.of(v, zoneId)
  def toZonedDateTime(v: LocalDate, zoneId: ZoneId): ZonedDateTime = ZonedDateTime.of(v, LocalTime.MIDNIGHT, zoneId)

  implicit def _toLocalDateTimeWrapper(v: LocalDateTime): LocalDateTimeWrapper = new LocalDateTimeWrapper(v)
  implicit def _toLocalDateWrapper(v: LocalDate): LocalDateWrapper = new LocalDateWrapper(v)

  implicit def dateWrapper(v: LocalDate): LocalDateWrapper = new LocalDateWrapper(v)
  implicit def dateWrapper(v: LocalDateTime): LocalDateTimeWrapper = new LocalDateTimeWrapper(v)
  implicit def dateWrapper(v: DateTimeFormatter): DateTimeFormatterWrapper = new DateTimeFormatterWrapper(v)

  //
  // =================================== LocalDateWrapper Wrapper ===================================
  //
  class LocalDateWrapper(v: LocalDate) {
    def toInstant(zoneId: ZoneId): Instant = v.atStartOfDay(zoneId).toInstant
    def toTimestamp(zoneId: ZoneId): Timestamp = Timestamp.from(toInstant(zoneId))
    def toDate(zoneId: ZoneId): Date = Date.from(toInstant(zoneId))
    def toMillis(zoneId: ZoneId): Long = toInstant(zoneId).toEpochMilli
    def toLocalDateTime: LocalDateTime = LocalDateTime.of(v, LocalTime.MIDNIGHT)
    def toZonedDateTime(zoneId: ZoneId): ZonedDateTime = ZonedDateTime.of(v, LocalTime.MIDNIGHT, zoneId)
  }

  //
  // =================================== LocalDateTimeWrapper Wrapper ===================================
  //
  class LocalDateTimeWrapper(v: LocalDateTime) {
    def toInstant(zoneId: ZoneId): Instant = v.atZone(zoneId).toInstant
    def toTimestamp(zoneId: ZoneId): Timestamp = Timestamp.from(toInstant(zoneId))
    def toDate(zoneId: ZoneId): Date = Date.from(toInstant(zoneId))
    def toMillis(zoneId: ZoneId): Long = toInstant(zoneId).toEpochMilli
    def toZonedDateTime(zoneId: ZoneId): ZonedDateTime = ZonedDateTime.of(v, zoneId)
  }

  //
  // =================================== LocalDateTimeWrapper Wrapper ===================================
  //
  class DateTimeFormatterWrapper(v: DateTimeFormatter) {
    def parseLocalDate(text: CharSequence): LocalDate = LocalDate.parse(text, v)
    def parseLocalDateTime(text: CharSequence): LocalDateTime = LocalDateTime.parse(text, v)
  }
}
