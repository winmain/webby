package webby.commons.text
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class LetterWidthsTest extends FunSuite with Matchers with TableDrivenPropertyChecks {

  val testCWU = new CharWidthUnion(3, 10, Seq(new CharWidths('a', 'f', Array(5, 6, 7, 8, 9, 10))))

  test("stringBreakFinder") {
    Table[String, Int, Int, Option[Int]](("given string", "given start", "column width", "result")
      , ("aaa", 0, 100, None)
      , ("aaa aaa", 0, 100, None)
      , ("fffff", 0, 30, None)
      , ("fff ff", 0, 25, Some(4))
      , ("fff ff", 4, 25, None)
      , ("abc\nabc", 0, 500, Some(4))
      , ("abc\n\nabc", 4, 500, Some(5))
      , ("abc abc abc abc", 0, 5 + 6 + 7 + 3 + 5 + 6 + 7, Some(8))
      , ("abc abc abc abc", 0, 5 + 6 + 7 + 3 + 5 + 6 + 7 + 3, Some(8))
      , ("abc abc abc abc", 0, 5 + 6 + 7 + 3 + 5 + 6 + 7 + 3 + 5, Some(8))
      , ("abc abc abc abc", 0, 5 + 6 + 7 + 3 + 5 + 6 + 7 + 3 + 5 + 6 + 7, Some(12))
    ).forEvery {case (str, start, colWidth, result) =>
      testCWU.stringBreakFinder(str, colWidth, start) shouldEqual result
    }
  }

  test("stringBreaks") {
    Table[String, Int, Seq[Int]](("given string", "column width", "result")
      , ("aaa", 100, Nil)
      , ("fffff", 30, Nil)
      , ("fff ff", 25, List(4))
      , ("abc abc abc abc", 10, List(4, 8, 12))
    ).forEvery {case (str, colWidth, result) =>
      testCWU.stringBreaks(str, colWidth).toList shouldEqual result
    }
  }
}
