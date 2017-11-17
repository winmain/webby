package orm.elasticsearch.utils

import java.time.LocalDate

import orm.elasticsearch.LocalDateIntConv

object DateTimeConverters {
  /** Дата в формате yyyymmdd */
  object Date extends LocalDateIntConv {
    // Такие манипуляции с созданием LocalDateTime и потом его конвертированием в toDateMidnight нужны, чтобы избежать
    // ошибки при инициализации даты, которая попадает в момент перевода часов (летнее-зимнее время)
    // Например, код new org.joda.time.LocalDate(1984, 4, 1) выдаст ошибку.
    def fromInt(j: Int): LocalDate = LocalDate.of(j / 10000, (j / 100) % 100, j % 100)
    def toInt(v: LocalDate): Int = v.getYear * 10000 + v.getMonthValue * 100 + v.getDayOfMonth
  }

  /** Дата в формате yyyymm */
  object YearMonth extends LocalDateIntConv {
    def fromInt(j: Int): LocalDate = LocalDate.of(j / 100, j % 100, 1)
    def toInt(v: LocalDate): Int = v.getYear * 100 + v.getMonthValue
  }
}
