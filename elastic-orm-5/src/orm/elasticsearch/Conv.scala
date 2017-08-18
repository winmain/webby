package orm.elasticsearch

import java.time._

import webby.commons.time.StdDates

// ------------------------------- Conv -------------------------------

/**
  * Values converter to/from Elastic format
  */
trait Conv[S] {
  /**
    * Read value from Elastic
    */
  def from(j: AnyRef): S

  /**
    * Write value to Elastic
    */
  def to(v: S): AnyRef
}

sealed class AsIs[S] extends Conv[S] {
  override def from(j: AnyRef): S = j.asInstanceOf[S]
  override def to(v: S): AnyRef = v.asInstanceOf[AnyRef]
}

sealed class NullableAsIs[S] extends Conv[S] {
  override def from(j: AnyRef): S = j.asInstanceOf[S]
  override def to(v: S): AnyRef = if (v == null) null else v.asInstanceOf[AnyRef]
}

sealed class AsOption[T] extends Conv[Option[T]] {
  override def from(j: AnyRef): Option[T] = if (j == null) None else Some(j.asInstanceOf[T])
  override def to(v: Option[T]): AnyRef = v match {
    case None => null
    case Some(value) => value.asInstanceOf[AnyRef]
  }
}


// ------------------------------- SimpleConv -------------------------------

trait SimpleConv[S] {
  /**
    * Read value from Elastic
    */
  def from(j: AnyRef): S

  /**
    * Write value to Elastic
    */
  def to(v: S): Any
}

object LocalDateTimeTimestampConv extends SimpleConv[LocalDateTime] {
  override def from(j: AnyRef): LocalDateTime = j.asInstanceOf[Long] match {
    case millis if millis < 253402300800000L => StdDates.toLocalDateTime(millis, ZoneId.systemDefault()) // Старый формат хранения данных в epochMillis
    case packed => LocalDateTime.of(LocalDateTimestampConv.unpackDate(packed), unpackTime(packed))
  }
  override def to(v: LocalDateTime): Any = LocalDateTimestampConv.packDate(v.toLocalDate) + packTime(v.toLocalTime)

  def packTime(v: LocalTime): Long =
    v.getNano / 1000000L + v.getSecond * 1000L + v.getMinute * 100000L + v.getHour * 10000000L
  def unpackTime(v: Long): LocalTime =
    LocalTime.of(((v / 10000000L) % 100L).toInt, ((v / 100000L) % 100L).toInt, ((v / 1000L) % 100L).toInt, (v % 1000L).toInt * 1000000)
}

object LocalDateTimestampConv extends SimpleConv[LocalDate] {
  override def from(j: AnyRef): LocalDate = j.asInstanceOf[Long] match {
    case millis if millis < 253402300800000L => StdDates.toLocalDate(millis, ZoneId.systemDefault()) // Старый формат хранения данных в epochMillis
    case packed => unpackDate(packed)
  }
  override def to(v: LocalDate): Any = packDate(v)

  def packDate(v: LocalDate): Long =
    v.getDayOfMonth * 1000000000L + v.getMonthValue * 100000000000L + v.getYear * 10000000000000L
  def unpackDate(v: Long): LocalDate =
    LocalDate.of((v / 10000000000000L).toInt, ((v / 100000000000L) % 100L).toInt, ((v / 1000000000L) % 100L).toInt)
}

trait LocalDateIntConv extends SimpleConv[LocalDate] {
  final override def from(j: AnyRef): LocalDate = fromInt(j.asInstanceOf[Int])
  final override def to(v: LocalDate): Any = toInt(v)

  def fromInt(j: Int): LocalDate
  def toInt(v: LocalDate): Int
}

object InstantEpochMillisConv extends SimpleConv[Instant] {
  override def from(j: AnyRef): Instant = Instant.ofEpochMilli(j.asInstanceOf[Long])
  override def to(v: Instant): Any = v.toEpochMilli
}

// ------------------------------- ObjectConv -------------------------------

sealed trait ObjectConv[Class, Write] {
  /**
    * Read value from Elastic
    */
  def from(j: AnyRef): Class

  /**
    * Write value to Elastic
    */
  def to(v: Write): AnyRef
}
