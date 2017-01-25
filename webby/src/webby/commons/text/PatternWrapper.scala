package webby.commons.text

import java.util.regex.{Matcher, Pattern}

//
// =================================== Pattern Wrapper ===================================
//
class PatternWrapper(val pat: Pattern) {

  /**
    * Searches through a target for a matching substring, starting from just after the end of last match.
    * If there wasn't any search performed, starts from zero.
    * @return <code>true</code> if a match found.
    */
  def find(s: String): Boolean = pat.matcher(s).find()

  /**
    * Attempts to match the entire region against the pattern.
    *
    * @return <tt>true</tt> if, and only if, the entire region sequence
    *         matches this matcher's pattern
    */
  def matches(s: String): Boolean = pat.matcher(s).matches()

  /**
    * Returns a matcher for a specified string.
    */
  def matcher(s: String): Matcher = pat.matcher(s)

  override def toString: String = pat.toString

  // -------- scala definitions --------

  /**
    * Сопоставляет регулярку с заданной строкой, и если она совпала (matches), то возаращается Some(Matcher), иначе None.
    */
  def mapMatch(s: String): Option[Matcher] = {
    val m = pat.matcher(s)
    if (m.matches()) Some(m) else None
  }
  /** Сопоставляет регулярку с заданной строкой и возвращает значения первой группы */
  def mapMatch1(s: String): Option[String] = {
    val m = pat.matcher(s)
    if (m.matches()) Some(m.group(1)) else None
  }
  /** Сопоставляет регулярку с заданной строкой и возвращает значения 1, 2 группы */
  def mapMatch2(s: String): Option[(String, String)] = {
    val m = pat.matcher(s)
    if (m.matches()) Some((m.group(1), m.group(2))) else None
  }
  /** Сопоставляет регулярку с заданной строкой и возвращает значения 1, 2, 3 группы */
  def mapMatch3(s: String): Option[(String, String, String)] = {
    val m = pat.matcher(s)
    if (m.matches()) Some((m.group(1), m.group(2), m.group(3))) else None
  }

  /**
    * Ищет регулярку в заданной строке, и если она нашлась (find), то возвращается Some(Matcher), иначе None.
    */
  def mapFind(s: String): Option[Matcher] = {
    val m = pat.matcher(s)
    if (m.find()) Some(m) else None
  }

  /**
    * Ищет регулярку в заданной строке, начиная с индекса #startIdx, и если она нашлась (find), то возаращается Some(Matcher), иначе None.
    */
  def mapFind(s: String, startIdx: Int): Option[Matcher] = {
    val m = pat.matcher(s)
    if (m.find(startIdx)) Some(m) else None
  }

  /** Поиск регулярки в заданной строке и возврат значения первой группы */
  def mapFind1(s: String): Option[String] = {
    val m = pat.matcher(s)
    if (m.find()) Some(m.group(1)) else None
  }
  /** Поиск регулярки в заданной строке и возврат значения 1, 2 группы */
  def mapFind2(s: String): Option[(String, String)] = {
    val m = pat.matcher(s)
    if (m.find()) Some((m.group(1), m.group(2))) else None
  }
  /** Поиск регулярки в заданной строке и возврат значения 1, 2, 3 группы */
  def mapFind3(s: String): Option[(String, String, String)] = {
    val m = pat.matcher(s)
    if (m.find()) Some((m.group(1), m.group(2), m.group(3))) else None
  }

  def unapplySeq(target: String): Option[Seq[String]] = {
    val m = pat.matcher(target)
    if (m.matches()) Some(1.until(m.groupCount()).map(i => m.group(i)))
    else None
  }
}
