package webby.commons.collection

import java.time.{LocalDate, LocalDateTime}

object Orderings {
  //
  // ----------------------------------- Orderings -----------------------------------
  //
  implicit object LDT extends Ordering[LocalDateTime] {
    def compare(x: LocalDateTime, y: LocalDateTime): Int = x.compareTo(y)
  }
  object LDTRev extends Ordering[LocalDateTime] {
    def compare(x: LocalDateTime, y: LocalDateTime): Int = y.compareTo(x)
  }

  implicit object LD extends Ordering[LocalDate] {
    def compare(x: LocalDate, y: LocalDate): Int = x.compareTo(y)
  }
  object LDRev extends Ordering[LocalDate] {
    def compare(x: LocalDate, y: LocalDate): Int = y.compareTo(x)
  }

}
