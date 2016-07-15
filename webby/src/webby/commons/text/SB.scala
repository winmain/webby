package webby.commons.text

import java.lang.StringBuilder

/**
 * Класс для удобного составления строк (замена scala StringBuilder)
 */
class SB(capacity: Int = 16) extends Appendable with CharSequence {self =>
  val sb = new StringBuilder(capacity)
  override def toString: String = { beforeResult(); sb.toString }
  def str: String = toString

  def me: SB = this
  def isEmpty: Boolean = length() == 0
  def nonEmpty: Boolean = !isEmpty

  def +(v: String): this.type = { beforeAppend(); sb append v; this }
  def ++(v: String): this.type = { beforeAppend(); sb append v; this }
  def +(v: Option[String]): this.type = { beforeAppend(); v.foreach(sb.append); this }
  def +(v: Char): this.type = { beforeAppend(); sb append v; this }
  def ++(v: Char): this.type = { beforeAppend(); sb append v; this }
  def +(v: Boolean): this.type = { beforeAppend(); sb append v; this }
  def ++(v: Boolean): this.type = { beforeAppend(); sb append v; this }
  def +(v: Int): this.type = { beforeAppend(); sb append v; this }
  def ++(v: Int): this.type = { beforeAppend(); sb append v; this }
  def +(v: Long): this.type = { beforeAppend(); sb append v; this }
  def ++(v: Long): this.type = { beforeAppend(); sb append v; this }
  def +(v: Float): this.type = { beforeAppend(); sb append v; this }
  def ++(v: Float): this.type = { beforeAppend(); sb append v; this }
  def +(v: Double): this.type = { beforeAppend(); sb append v; this }
  def ++(v: Double): this.type = { beforeAppend(); sb append v; this }
  def +(v: CharSequence): this.type = { beforeAppend(); sb append v; this }
  def ++(v: CharSequence): this.type = { beforeAppend(); sb append v; this }

  implicit sealed class StringWrapper(val s: String) {
    def unary_+ : self.type = { beforeAppend(); sb append s; self }
  }
  implicit sealed class StringOptionWrapper(val s: Option[String]) {
    def unary_+ : self.type = { beforeAppend(); s.foreach(sb.append); self }
  }

  def back(chars: Int): this.type = { sb.delete(sb.length() - chars, sb.length()); this }

  override def append(c: Char): this.type = { beforeAppend(); sb append c; this }
  override def append(v: CharSequence): this.type = { beforeAppend(); sb append v; this }
  override def append(v: CharSequence, start: Int, end: Int): this.type = { beforeAppend(); sb.append(v, start, end); this }

  override def charAt(index: Int): Char = sb.charAt(index)
  override def length(): Int = sb.length()
  override def subSequence(start: Int, end: Int): CharSequence = sb.subSequence(start, end)

  protected def beforeAppend(): Unit = {}
  protected def beforeResult(): Unit = {}
}

object SB {
  def apply: SB = new SB
}
