package orm.elasticsearch

import java.time._
import java.{lang => jl, util => ju}

import org.elasticsearch.common.geo.GeoPoint
import webby.commons.time.StdDates

// ------------------------------- Conv -------------------------------

/**
  * Values converter to/from Elastic format
  */
trait Conv[W, R] {
  /**
    * Read value from Elastic
    */
  def from(j: AnyRef): R

  /**
    * Write value to Elastic
    */
  def to(v: W): AnyRef
}

trait ConvOne[S] extends Conv[S, S]

sealed class AsIs[W, R] extends Conv[W, R] {
  override def from(j: AnyRef): R = j.asInstanceOf[R]
  override def to(v: W): AnyRef = v.asInstanceOf[AnyRef]
}

sealed class NullableAsIs[W, R] extends Conv[W, R] {
  override def from(j: AnyRef): R = j.asInstanceOf[R]
  override def to(v: W): AnyRef = if (v == null) null else v.asInstanceOf[AnyRef]
}

/** For short numbers Elastic returns Integer instead of Long */
object LongConv extends Conv[Long, Long] {
  override def from(j: AnyRef): Long = j match {
    case v: jl.Number => v.longValue()
    case null => 0
  }
  override def to(v: Long): AnyRef = jl.Long.valueOf(v)
}

sealed class AsOption[W, R] extends Conv[Option[W], Option[R]] {
  override def from(j: AnyRef): Option[R] = if (j == null) None else Some(j.asInstanceOf[R])
  override def to(v: Option[W]): AnyRef = v match {
    case None => null
    case Some(value) => value.asInstanceOf[AnyRef]
  }
}

/** Treats empty map in code as null in elastic.
  * Conversions followed:
  * [elastic] -- [code]
  * null      -> {}
  * null      <- {}
  * null      <- null
  */
sealed class EmptyMapAsNull extends ConvOne[ju.Map[String, Any]] {
  override def from(j: AnyRef): ju.Map[String, Any] = if (j == null) ju.Collections.emptyMap() else j.asInstanceOf[ju.Map[String, Any]]
  override def to(v: ju.Map[String, Any]): AnyRef = if (v == null || v.isEmpty) null else v
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


object GeoPointConv extends SimpleConv[GeoPoint] {
  override def from(j: AnyRef): GeoPoint = j match {
    case map: ju.HashMap[_, _] =>
      val lon = map.get("lon").asInstanceOf[Double]
      val lat = map.get("lat").asInstanceOf[Double]
      new GeoPoint(lat, lon)

    case geoPoint: GeoPoint => geoPoint
  }
  override def to(v: GeoPoint): Any = v
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
