package webby.api.libs

import org.scalatest.{Matchers, WordSpec}

class CryptoSignerTest extends WordSpec with Matchers {

  "CryptoSigner" should {
    "be able to sign and verify String using HMAC-SHA1 algorithm" in {
      val text = "Webby Framework"
      val key = "0123456789abcdef"

      val signer = new CryptoSigner(key.getBytes)
      val composed = signer.signCompose(text)
      composed shouldEqual "8778359c71b07f98ea1f3fb2c6fcabc468dbf606Webby Framework"

      signer.verifyComposed(composed) shouldEqual Some(text)
    }

    "be able to sign and verify Long message using HMAC-SHA1 algorithm" in {
      val key = "0123456789abcdef"
      val signer = new CryptoSigner(key.getBytes)
      val message = 0x12345678abcdef99L

      val composed = signer.signComposeLong(message)
      composed shouldEqual "ab9f0cbe088bf246902e6de966e5460faad21af79ys742ug966x"

      signer.verifyComposedLong(composed) shouldEqual Some(message)
    }
  }

}

