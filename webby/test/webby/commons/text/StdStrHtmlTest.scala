package webby.commons.text
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class StdStrHtmlTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  test("htmlCapitalize") {
    Table[String, String](("given string", "result")
      , (null.asInstanceOf[String], null.asInstanceOf[String])
      , ("", "")
      , ("qwe", "Qwe")
      , (" abc", " abc")
      , ("<tag", "<tag")
      , ("<em>", "<em>")
      , ("<em>text", "<em>Text")
      , ("</br>zzz", "</br>Zzz")
      , ("</br>ZZZ", "</br>ZZZ")
      , ("<two><tags>text", "<two><tags>Text")
      , ("<two><tags>", "<two><tags>")
      , ("русский текст", "Русский текст")
    ).forEvery {case (str, result) =>
      StdStrHtml.htmlCapitalize(str) shouldEqual result
    }
  }
}
