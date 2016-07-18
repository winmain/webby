package webby.commons.text.validator

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class EmailValidatorTest extends FunSuite with TableDrivenPropertyChecks with Matchers {
  test("isValidInternetAddress") {
    Table[String, Boolean](("email", "valid")
      , ("rus.shishkov@mail-ru", false)
      , ("katena.bauer.@mail.ru", true)
      , ("denischernyev97@mail", false)
      , ("al.altunina2015@yandex", false)
      , ("sineva1983@ru", false)
      , ("zanin@hardware.kras.ru", true)
      , ("IVZavgorodnyaya@stu.rosbank.ru", true)
      , ("qwe@mail.ru", true)
      , ("qwe@.mail.ru", false)
      , ("qwe@mail.ru.", false)
      , ("--Gipsohsaid@gmail.com", false)
      , ("-invalid@gmail.com", false)
      , ("a-correct-email@gmail.com", true)
    ).forEvery {case (email: String, valid: Boolean) =>
      EmailValidator.isValidInternetAddress(email) shouldEqual valid
    }
  }
}
