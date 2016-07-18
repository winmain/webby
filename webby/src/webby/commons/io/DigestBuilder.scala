package webby.commons.io
import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils

/**
  * Builder, основанный на [[MessageDigest]].
  * Аналогичен билдеру [[webby.commons.text.SB]]
  */
class DigestBuilder(val md: MessageDigest) {self =>
  override def toString: String = Hex.encodeHexString(bytes)
  def str: String = toString
  def bytes: Array[Byte] = md.digest()

  def update(v: Array[Byte]): this.type = {md.update(v); this}
  def update(v: Array[Byte], length: Int): this.type = {md.update(v, 0, length); this}
  def update(v: Array[Byte], offset: Int, length: Int): this.type = {md.update(v, offset, length); this}
  def update(v: String): this.type = update(v.getBytes)
  def update(v: Option[String]): this.type = {v.foreach(s => md.update(s.getBytes)); this}

  def +(v: Array[Byte]): this.type = update(v)
  def +(v: String): this.type = update(v)
  def +(v: Option[String]): this.type = update(v)

  implicit sealed class ByteArrayWrapper(val v: Array[Byte]) {
    def unary_+ : self.type = {md.update(v); self}
  }
  implicit sealed class StringWrapper(val s: String) {
    def unary_+ : self.type = {md.update(s.getBytes); self}
  }
  implicit sealed class StringOptionWrapper(val s: Option[String]) {
    def unary_+ : self.type = {s.foreach(ss => md.update(ss.getBytes)); self}
  }
}

/**
  * MD5-builder, создаёт md5 из строки
  */
class MD5 extends DigestBuilder(DigestUtils.getMd5Digest)
