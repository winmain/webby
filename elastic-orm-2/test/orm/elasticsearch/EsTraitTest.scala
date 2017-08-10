package orm.elasticsearch
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneId}

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}
import webby.commons.time.StdDates

class EsTraitTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  test("LocalDateTimeTimestampConv packTime") {
    Table[LocalTime, Long](("LocalTime", "packed")
      , (LocalTime.of(19, 55, 1, 135), 195501000L)
      , (LocalTime.of(3, 25, 52, 123456789), 32552123L)
      , (LocalTime.of(0, 0, 0, 0), 0L)
    ).forEvery {case (localTime, packed) =>
      LocalDateTimeTimestampConv.packTime(localTime) shouldEqual packed
    }
  }

  test("LocalDateTimeTimestampConv unpackTime") {
    Table[Long, LocalTime](("packed", "LocalTime")
      , (195501000L, LocalTime.of(19, 55, 1, 0))
      , (32552123L, LocalTime.of(3, 25, 52, 123000000))
      , (0L, LocalTime.of(0, 0, 0, 0))
    ).forEvery {case (packed, localTime) =>
      LocalDateTimeTimestampConv.unpackTime(packed) shouldEqual localTime
    }
  }

  test("LocalDateTimeTimestampConv from") {
    Table[Long, LocalDateTime](("packed", "LocalDateTime")
      , (1447859405000L, StdDates.toLocalDateTime(1447859405000L, ZoneId.systemDefault())) // old format
      , (20151125032552123L, LocalDateTime.of(2015, 11, 25, 3, 25, 52, 123000000)) // new format
    ).forEvery {case (packed, localDateTime) =>
      LocalDateTimeTimestampConv.from(packed.asInstanceOf[AnyRef]) shouldEqual localDateTime
    }
  }

  test("LocalDateTimestampConv packDate/unpackDate") {
    Table[LocalDate, Long](("LocalDate", "packed")
      , (LocalDate.of(2005, 6, 5), 20050605000000000L)
      , (LocalDate.of(2015, 11, 25), 20151125000000000L)
      , (LocalDate.of(0, 1, 1), 101000000000L)
    ).forEvery {case (localDate, packed) =>
      LocalDateTimestampConv.packDate(localDate) shouldEqual packed
      LocalDateTimestampConv.unpackDate(packed) shouldEqual localDate
    }
  }

  test("LocalDateTimestampConv from") {
    Table[Long, LocalDate](("packed", "LocalDate")
      , (1447859405000L, StdDates.toLocalDate(1447859405000L, ZoneId.systemDefault())) // old format
      , (20151125032552123L, LocalDate.of(2015, 11, 25)) // new format
    ).forEvery {case (packed, localDate: LocalDate) =>
      LocalDateTimestampConv.from(packed.asInstanceOf[AnyRef]) shouldEqual localDate
    }
  }
}
