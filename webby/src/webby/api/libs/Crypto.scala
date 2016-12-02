package webby.api.libs

import javax.crypto._
import javax.crypto.spec.SecretKeySpec

import com.google.common.base.Charsets
import org.apache.commons.codec.binary.Hex
import webby.api.{App, WebbyException}

/**
  * Cryptographic utilities.
  *
  * Requires sbt dependency
  * {{{
  *   deps += "commons-codec" % "commons-codec" % "1.10"
  * }}}
  */
@deprecated("Use CryptoSigner", "0.3.0")
object Crypto {

  private lazy val secret: Option[String] = App.maybeApp.flatMap(_.configuration.getString("application.secret"))
  private lazy val secretBytes: Option[Array[Byte]] = secret.map(_.getBytes(Charsets.UTF_8))

  /**
    * Signs the given String with HMAC-SHA1 using the given key.
    */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Hex.encodeHexString(mac.doFinal(message.getBytes(Charsets.UTF_8)))
  }

  /**
    * Signs the given String with HMAC-SHA1 using the application’s secret key.
    */
  def sign(message: String): String = {
    val sb = secretBytes.getOrElse(throw new WebbyException("Configuration error", "Missing application.secret"))
    sign(message, sb)
  }

  /**
    * Encrypt a String with the AES encryption standard using the application secret
    * @param value The String to encrypt
    * @return An hexadecimal encrypted string
    */
  def encryptAES(value: String): String = {
    secret.map(secret => encryptAES(value, secret.substring(0, 16))).getOrElse {
      throw new WebbyException("Configuration error", "Missing application.secret")
    }
  }

  /**
    * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
    * @param value      The String to encrypt
    * @param privateKey The key used to encrypt
    * @return An hexadecimal encrypted string
    */
  def encryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Hex.encodeHexString(cipher.doFinal(value.getBytes(Charsets.UTF_8)))
  }

  /**
    * Decrypt a String with the AES encryption standard using the application secret
    * @param value An hexadecimal encrypted string
    * @return The decrypted String
    */
  def decryptAES(value: String): String = {
    secret.map(secret => decryptAES(value, secret.substring(0, 16))).getOrElse {
      throw new WebbyException("Configuration error", "Missing application.secret")
    }
  }

  /**
    * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
    * @param value      An hexadecimal encrypted string
    * @param privateKey The key used to encrypt
    * @return The decrypted String
    */
  def decryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Hex.decodeHex(value.toCharArray)))
  }


  def verifyHmac(cookieValue: String): Option[String] = {
    val (hmac, value) = cookieValue.splitAt(40)
    if (Crypto.sign(value) == hmac) Some(value) else None
  }

  def verifyHmacLong(cookieValue: String): Option[Long] = {
    verifyHmac(cookieValue).map {tokenStr =>
      try {
        java.lang.Long.parseLong(tokenStr, 36)
      } catch {case e: NumberFormatException => return None}
    }
  }

  def signHmacLong(token: Long): String = {
    val tokenStr = java.lang.Long.toString(token, 36)
    sign(tokenStr) + tokenStr
  }
}
