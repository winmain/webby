package webby.commons.date

import java.time.LocalDate

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class PeriodTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  def d(day: Int) = LocalDate.of(2015, 1, day)
  def p(day1: Int, day2: Int) = Period(d(day1), d(day2))

  test("test empty period") {
    val period: Period = p(1, 1)
    period.days shouldEqual 0
  }

  test("test contains") {
    Table[Period, Period, Boolean](("period1", "period2", "result")
      , (p(1, 1), p(1, 1), true)
      , (p(1, 1), p(1, 2), false)
      , (p(1, 2), p(1, 1), true)
      , (p(1, 2), p(2, 2), true)
      , (p(1, 1), p(2, 2), false)
      , (p(1, 2), p(3, 4), false)
      , (p(1, 3), p(2, 4), false)
      , (p(1, 3), p(3, 4), false)
      , (p(1, 3), p(4, 6), false)
      , (p(1, 5), p(1, 5), true)
      , (p(1, 5), p(1, 4), true)
      , (p(1, 5), p(2, 4), true)
      , (p(3, 7), p(2, 7), false)
      , (p(3, 7), p(2, 9), false)
      , (p(5, 9), p(2, 8), false)
      , (p(5, 9), p(2, 5), false)
      , (p(5, 9), p(2, 4), false)
    ).forEvery {case (period1, period2, result) =>
      period1.contains(period2) shouldEqual result
    }
  }

  test("test intersects") {
    Table[Period, Period, Boolean](("period1", "period2", "result")
      , (p(1, 1), p(1, 1), false)
      , (p(1, 1), p(1, 2), false)
      , (p(1, 2), p(1, 1), false)
      , (p(1, 2), p(2, 2), false)
      , (p(1, 1), p(2, 2), false)
      , (p(1, 2), p(3, 4), false)
      , (p(1, 3), p(2, 4), true)
      , (p(1, 3), p(3, 4), false)
      , (p(1, 3), p(4, 6), false)
      , (p(1, 5), p(1, 5), true)
      , (p(1, 5), p(1, 4), true)
      , (p(1, 5), p(2, 4), true)
      , (p(3, 7), p(2, 7), true)
      , (p(3, 7), p(2, 9), true)
      , (p(5, 9), p(2, 8), true)
      , (p(5, 9), p(2, 5), false)
      , (p(5, 9), p(2, 4), false)
    ).forEvery {case (period1, period2, result) =>
      period1.intersects(period2) shouldEqual result
    }
  }

  test("test intersectOrAdjacent") {
    Table[Period, Period, Boolean](("period1", "period2", "result")
      , (p(1, 1), p(1, 1), true)
      , (p(1, 1), p(1, 2), true)
      , (p(1, 2), p(1, 1), true)
      , (p(1, 2), p(2, 2), true)
      , (p(1, 1), p(2, 2), false)
      , (p(1, 2), p(3, 4), false)
      , (p(1, 3), p(2, 4), true)
      , (p(1, 3), p(3, 4), true)
      , (p(1, 3), p(4, 6), false)
      , (p(1, 5), p(1, 5), true)
      , (p(1, 5), p(1, 4), true)
      , (p(1, 5), p(2, 4), true)
      , (p(3, 7), p(2, 7), true)
      , (p(3, 7), p(2, 9), true)
      , (p(5, 9), p(2, 8), true)
      , (p(5, 9), p(2, 5), true)
      , (p(5, 9), p(2, 4), false)
    ).forEvery {case (period1, period2, result) =>
      period1.intersectOrAdjacent(period2) shouldEqual result
    }
  }

  test("test tryGlue") {
    Table[Period, Period, Option[Period]](("period1", "period2", "result")
      , (p(1, 1), p(1, 1), Some(p(1, 1)))
      , (p(1, 1), p(1, 2), Some(p(1, 2)))
      , (p(1, 2), p(1, 1), Some(p(1, 2)))
      , (p(1, 2), p(2, 2), Some(p(1, 2)))
      , (p(1, 1), p(2, 2), None)
      , (p(1, 2), p(3, 4), None)
      , (p(1, 3), p(2, 4), Some(p(1, 4)))
      , (p(1, 3), p(3, 4), Some(p(1, 4)))
      , (p(1, 3), p(4, 6), None)
      , (p(1, 5), p(1, 5), Some(p(1, 5)))
      , (p(1, 5), p(1, 4), Some(p(1, 5)))
      , (p(1, 5), p(2, 4), Some(p(1, 5)))
      , (p(3, 7), p(2, 7), Some(p(2, 7)))
      , (p(3, 7), p(2, 9), Some(p(2, 9)))
      , (p(5, 9), p(2, 8), Some(p(2, 9)))
      , (p(5, 9), p(2, 5), Some(p(2, 9)))
      , (p(5, 9), p(2, 4), None)
    ).forEvery {case (period1, period2, result) =>
      period1.tryGlue(period2) shouldEqual result
    }
  }
}
