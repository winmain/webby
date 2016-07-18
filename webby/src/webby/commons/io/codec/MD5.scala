package webby.commons.io.codec

import de.xam.md5.{MD5 => xamMD5}

/**
  * Простенький класс для работы с MD5.
  *
  * Для его использования нужно добавить библиотеку {{{
  * deps += "de.xam" % "md5" % "2.6.3"
  * }}}
  */
object MD5 {
  /**
   * Получить шестнадцатеричный md5-хеш заданной строки
   */
  def hex(str: String): String = {
    val md5 = new xamMD5
    md5.Update(str)
    xamMD5.asHex(md5.Final)
  }

  def hex(bytes: Array[Byte]): String = {
    val md5 = new xamMD5
    md5.Update(bytes)
    xamMD5.asHex(md5.Final)
  }
}


/**
 * MD5-builder, создаёт md5 из строки
 */
class MD5 {self =>
  private val md5: xamMD5 = new xamMD5

  override def toString: String = xamMD5.asHex(md5.Final)
  def str: String = toString
  def bytes: Array[Byte] = md5.Final()

  def +(v: String): this.type = { md5.Update(v); this }
  def ++(v: String): this.type = { md5.Update(v); this }
  def +(v: Option[String]): this.type = { v.foreach(md5.Update); this }
  def update(v: Array[Byte]): this.type = { md5.Update(v); this }
  def update(v: Array[Byte], length: Int): this.type = { md5.Update(v, length); this }
  def update(v: Array[Byte], offset: Int, length: Int): this.type = { md5.Update(v, offset, length); this }

  implicit sealed class StringWrapper(val s: String) {
    def unary_+ : self.type = { md5.Update(s); self }
  }
  implicit sealed class StringOptionWrapper(val s: Option[String]) {
    def unary_+ : self.type = { s.foreach(md5.Update); self }
  }

}