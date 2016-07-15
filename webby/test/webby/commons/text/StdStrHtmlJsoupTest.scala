package webby.commons.text
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class StdStrHtmlJsoupTest extends FunSuite with Matchers with TableDrivenPropertyChecks {
  test("stripHtmlTags") {
    Table[String, String](("given string", "result")
      , ("", "")
      , ("test qweqwe", "test qweqwe")
      , ("    more    spaces   ", "more spaces")
      , ("string\nbreak", "string break")
      , ("string <br>\n break", "string  break")
      , ("some <b>tags</b> with <a href=\"http://jopa.ru\">links</a>", "some tags with links")
    ).forEvery {case (str, result) =>
      StdStrHtmlJsoup.stripHtmlTags(str) shouldEqual result
    }
  }

  test("stripHtmlTagsPreserveLineBreaks") {
    Table[String, String](("given string", "result")
      , ("", "")
      , ("test abc", "test abc")
      , ("    more    spaces   ", "more spaces")
      , ("simple\rstring\nbreak", "simple string break")
      , ("string<br>break", "string\nbreak")
      , ("string<br>\nbreak", "string\nbreak")
      , ("some <b>tags</b> with <a href=\"http://jopa.ru\">links</a>", "some tags with links")
      , ("here is a list<li>item1</li>", "here is a list\n• item1")
      , ("here is a list<li>item1</li><li>item2</li>\n<li>item3</li>", "here is a list\n• item1\n• item2 \n• item3")
    ).forEvery {case (str, result) =>
      StdStrHtmlJsoup.stripHtmlTagsPreserveLineBreaks(str) shouldEqual result
    }
  }
}
