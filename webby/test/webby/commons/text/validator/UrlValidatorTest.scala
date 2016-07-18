package webby.commons.text.validator

import java.net.URL

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class UrlValidatorTest extends FunSuite with TableDrivenPropertyChecks with Matchers {
  test("validatorTable") {
    Table[String, Boolean](("given url", "result")
      , ("", false)
      , (".", false)
      , ("a", false)
      , ("a.a", false)
      , ("aa.aa", false)
      , ("rosrabota.ru", false)
      , ("//rosrabota.ru", false)
      , ("http:/rosrabota.ru", false)
      , ("http://", false)
      , ("http://a", false)
      , ("http://a.a", false)
      , ("http://b.cc", false)
      , ("http://bb.c", false)
      , ("http://bb.cc", true)
      , ("http://rosrabota.ru", true)
      , ("http://росработа.рф", true)
      , ("https://rosrabota.ru", true)
      , ("ftp://rosrabota.ru", false)
    ).forEvery {case (givenUrl, result) =>
      UrlValidator.validate(givenUrl).isDefined shouldEqual result
    }
  }

  test("validateDomainTable") {
    Table[String, Array[String], Boolean](("given url", "allowed domains", "result")
      , ("http://rosrabota.ru", Array("vk.com"), false)
      , ("http://vk.com", Array("vk.com"), true)
      , ("http://www.vk.com", Array("vk.com"), true)
      , ("http://", Array[String](), false)
    ).forEvery {case (givenUrl, domains, result) =>
      UrlValidator.validateDomain(new URL(givenUrl), allowedDomains = domains) shouldEqual result
    }
  }
}
