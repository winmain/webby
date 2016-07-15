package webby.api.libs

import org.scalatest.{Matchers, WordSpec}

class CryptoTest extends WordSpec with Matchers {

  "Crypto api" should {
    "be able to encrypt/decrypt text using AES algorithm" in {
      val text = "Webby Framework"
      val key = "0123456789abcdef"
      Crypto.decryptAES(Crypto.encryptAES(text, key), key) shouldEqual text
    }
  }

}

