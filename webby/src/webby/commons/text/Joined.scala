package webby.commons.text

import java.lang.StringBuilder

/**
  * Класс для удобного конструирования строки из череды параметров, разделённых заданным разделителем.
  *
  * Пример:
  * {{{
  *   (Joined(", ") ++ sexString ++ age.format ++ expString).toString
  * }}}
  *
  * @param joinStr разделитель
  */
class Joined(joinStr: String) {

  val sb: StringBuilder = new StringBuilder()

  def append(str: String, joinWith: String): this.type = {
    if (str == null || str.isEmpty) this
    else {
      if (nonEmpty) sb.append(joinWith)
      _append(str)
      this
    }
  }
  def append(str: String): this.type = append(str, joinStr)

  def append(strOption: Option[String], joinWith: String): this.type = strOption match {
    case Some(str) => append(str, joinWith)
    case None => this
  }
  def append(strOption: Option[String]): this.type = append(strOption, joinStr)

  def appendIf(condition: Boolean, str: String): this.type = if (condition) append(str) else this
  def appendIf(condition: Boolean, str: Option[String]): this.type = if (condition) append(str) else this

  protected def _append(str: String) {
    sb.append(str)
  }

  def ++(str: String): this.type = append(str)
  def ++(strOption: Option[String]): this.type = strOption match {
    case Some(str) => append(str)
    case None => this
  }

  def isEmpty = sb.length == 0
  def nonEmpty = !isEmpty

  override def toString: String = sb.toString
  def str: String = toString

  def toStringOption: Option[String] = if (isEmpty) None else Some(toString)
}

/**
  * Версия JoinedAppender, которая дополнительно обрамляет вставляемую строку.
  */
class WrappedJoined(joinStr: String, wrapPre: String, wrapPost: String) extends Joined(joinStr) {

  override protected def _append(str: String) {
    sb.append(wrapPre)
    sb.append(str)
    sb.append(wrapPost)
  }
}

object Joined {
  def apply(joinStr: String): Joined = new Joined(joinStr)
}
