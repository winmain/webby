package webby.commons.text
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class StdStrTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  test("bigNumber") {
    Table[Int, String](("given number", "result")
      , (0, "0")
      , (100, "100")
      , (942, "942")
      , (1000, "1 000")
      , (10000, "10 000")
      , (42891, "42 891")
      , (948512, "948 512")
      , (1000000, "1 000 000")
      , (-1, "-1")
      , (-421, "-421")
      , (-1242, "-1 242")
    ).forEvery {case (num, result) =>
      StdStr.bigNumber(num, " ") shouldEqual result
    }
  }
}
