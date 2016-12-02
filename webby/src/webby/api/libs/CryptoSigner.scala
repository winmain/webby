package webby.api.libs
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import com.google.common.base.Charsets
import org.apache.commons.codec.binary.Hex

/**
  * Cryptographic signer using provided key and MAC algorithm
  *
  * Requires sbt dependency
  * {{{
  *   deps += "commons-codec" % "commons-codec" % "1.10"
  * }}}
  */
class CryptoSigner(key: Array[Byte], macAlgorithm: String = "HmacSHA1") {
  private val initialMac = Mac.getInstance(macAlgorithm)
  private val keySpec = new SecretKeySpec(key, macAlgorithm)

  /**
    * Signs the given array of bytes with HMAC-SHA1
    */
  def sign(message: Array[Byte]): Array[Byte] = {
    val mac = initialMac.clone().asInstanceOf[Mac]
    mac.init(keySpec)
    mac.doFinal(message)
  }

  /**
    * Signs the given String with HMAC-SHA1 and produce hex string
    */
  def signHex(message: String): String = {
    Hex.encodeHexString(sign(message.getBytes(Charsets.UTF_8)))
  }

  /**
    * Signs the given message to hex string and compose resulting message as `sign` + `message`
    */
  def signCompose(message: String): String = {
    signHex(message) + message
  }

  /**
    * Verify signed message consisting of `sign` + `message`.
    * Returns message if sign is correct.
    */
  def verifyComposed(composed: String): Option[String] = {
    if (composed.length > 40) {
      val hmac = composed.substring(0, 40)
      val value = composed.substring(40)
      if (signHex(value) == hmac) Some(value) else None
    } else {
      None
    }
  }

  /**
    * Signs the given message to hex string and compose resulting message as `sign` + `longMessage`.
    * LongMessage is Long with radix 36.
    */
  def signComposeLong(message: Long): String = {
    signCompose(java.lang.Long.toString(message, 36))
  }

  /**
    * Verify signed Long message consisting of `sign` + `longMessage`.
    * LongMessage is Long with radix 36.
    * Returns Long message if sign is correct.
    */
  def verifyComposedLong(composed: String): Option[Long] = {
    verifyComposed(composed).map {message =>
      try {
        java.lang.Long.parseLong(message, 36)
      } catch {case e: NumberFormatException => return None}
    }
  }
}
