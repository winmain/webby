package webby.commons.io.cookie
import java.lang.StringBuilder

import org.scalatest.{FunSuite, Matchers}

class CookieEncoderUtilV0Test extends FunSuite with Matchers {
  test("add() latin letters") {
    val sb = new StringBuilder()
    CookieEncoderUtilV0.add(sb, "foo", "bar")
    sb.toString shouldEqual "foo=bar; "
  }

  test("add() cyrillic letters") {
    val sb = new StringBuilder()
    CookieEncoderUtilV0.add(sb, "foo", "бар")
    sb.toString shouldEqual "foo=%D0%B1%D0%B0%D1%80; "
  }
}
