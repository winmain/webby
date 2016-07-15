package webby.commons.date

import java.sql.Timestamp
import java.time._
import java.time.temporal.{Temporal, TemporalAccessor, TemporalField, TemporalUnit}
import java.util.Date

trait DateTimeWrapper extends Temporal with TemporalAccessor {
  def year: Int
  def month: Int
  def day: Int
  def hour: Int
  def minute: Int
  def second: Int

  def yearOfCentury: Int = year % 100

  def toInstant(zoneId: ZoneId): Instant
  def toTimestamp(zoneId: ZoneId): Timestamp
  def toDate(zoneId: ZoneId): Date
  def toMillis(zoneId: ZoneId): Long
  def toLocalDateTime: LocalDateTime
  def toLocalDate: LocalDate
}

final class LocalDateTimeWrapper(v: LocalDateTime) extends DateTimeWrapper {
  override def year: Int = v.getYear
  override def month: Int = v.getMonthValue
  override def day: Int = v.getDayOfMonth
  override def hour: Int = v.getHour
  override def minute: Int = v.getMinute
  override def second: Int = v.getSecond

  override def toInstant(zoneId: ZoneId): Instant = StdDates.toInstant(v, zoneId)
  override def toTimestamp(zoneId: ZoneId): Timestamp = StdDates.toTimestamp(v, zoneId)
  override def toDate(zoneId: ZoneId): Date = StdDates.toDate(v, zoneId)
  override def toMillis(zoneId: ZoneId): Long = StdDates.toMillis(v, zoneId)
  override def toLocalDateTime: LocalDateTime = v
  override def toLocalDate: LocalDate = v.toLocalDate

  // --- Temporal ---
  override def plus(amountToAdd: Long, unit: TemporalUnit): Temporal = v.plus(amountToAdd, unit)
  override def until(endExclusive: Temporal, unit: TemporalUnit): Long = v.until(endExclusive, unit)
  override def isSupported(unit: TemporalUnit): Boolean = v.isSupported(unit)
  override def `with`(field: TemporalField, newValue: Long): Temporal = v.`with`(field, newValue)

  // --- TemporalAccessor ---
  override def isSupported(field: TemporalField): Boolean = v.isSupported(field)
  override def getLong(field: TemporalField): Long = v.getLong(field)
}

final class LocalDateWrapper(v: LocalDate) extends DateTimeWrapper {
  override def year: Int = v.getYear
  override def month: Int = v.getMonthValue
  override def day: Int = v.getDayOfMonth
  override def hour: Int = 0
  override def minute: Int = 0
  override def second: Int = 0

  override def toInstant(zoneId: ZoneId): Instant = StdDates.toInstant(v, zoneId)
  override def toTimestamp(zoneId: ZoneId): Timestamp = StdDates.toTimestamp(v, zoneId)
  override def toDate(zoneId: ZoneId): Date = StdDates.toDate(v, zoneId)
  override def toMillis(zoneId: ZoneId): Long = StdDates.toMillis(v, zoneId)
  override def toLocalDateTime: LocalDateTime = LocalDateTime.of(v, LocalTime.MIDNIGHT)
  override def toLocalDate: LocalDate = v

  // --- Temporal ---
  override def plus(amountToAdd: Long, unit: TemporalUnit): Temporal = v.plus(amountToAdd, unit)
  override def until(endExclusive: Temporal, unit: TemporalUnit): Long = v.until(endExclusive, unit)
  override def isSupported(unit: TemporalUnit): Boolean = v.isSupported(unit)
  override def `with`(field: TemporalField, newValue: Long): Temporal = v.`with`(field, newValue)

  // --- TemporalAccessor ---
  override def isSupported(field: TemporalField): Boolean = v.isSupported(field)
  override def getLong(field: TemporalField): Long = v.getLong(field)
}
