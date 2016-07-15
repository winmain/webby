package webby.commons.text
import org.apache.commons.lang3.StringUtils

abstract class StdStr {
  // ------------------------------- Endings -------------------------------

  /**
    * Поставить точку в конце строки, если её там нет. Также, делает trim строки.
    */
  def dotEnding(str: String): String = {
    val s = str.trim
    if (s.charAt(s.length - 1) != '.') s + "." else s
  }

  /**
    * Поставить многоточие в конце строки. Учитывает то, что там может стоять точка. Также, делает trim строки.
    */
  def ellipsisEnding(str: String): String = {
    StdCharMatchers.endingSymbols.trimTrailingFrom(str) + "..."
  }

  // ------------------------------- Url -------------------------------

  /** Дописать параметры типа a=123&b=qwe в конец урла с учётом того, что урл уже может содержать другие параметры */
  def appendUrlParams(url: String, params: String) = url + (if (url.indexOf('?') == -1) '?' else '&') + params

  // ------------------------------- Numbers -------------------------------

  /**
    * Форматировать число, добавляя заданную строку в разделители тысячных разрядов.
    */
  def bigNumber(num: Int, thousandSeparator: String): String = {
    val str = String.valueOf(math.abs(num))
    val len = str.length
    if (len > 3) {
      var start = 0
      var next = len % 3
      if (next == 0) next += 3
      val sb = new StringBuilder()
      if (num < 0) sb append '-'
      do {
        sb.append(str.substring(start, next))
        sb.append(thousandSeparator)
        start = next
        next += 3
      } while (next < len)
      sb.append(str.substring(start, next))
      sb.toString()
    } else {
      if (num < 0) "-" + str else str
    }
  }

  def bigNumberS(num: Int) = bigNumber(num, "<s></s>")
  def bigNumberHairSpace(num: Int) = bigNumber(num, "\u2009")
}

object StdStr extends StdStr

// ------------------------------- Str wrapper -------------------------------

trait StdStrWrapper {
  protected def s: String

  /** Поставить точку в конце строки, если её там нет. Также, делает trim строки. */
  def dotEnding: String = StdStr.dotEnding(s)

  /** Поставить многоточие в конце строки. Учитывает то, что там может стоять точка. Также, делает trim строки. */
  def ellipsisEnding: String = StdStr.ellipsisEnding(s)

  /** Дописать параметры типа a=123&b=qwe в конец урла с учётом того, что урл уже может содержать другие параметры */
  def appendUrlParams(params: String): String = StdStr.appendUrlParams(s, params)

  /** Вернуть расширение имени файла (например: doc, pdf, ...) */
  def fileNameExt: String = StringUtils.substringAfterLast(s, ".")
}

// ------------------------------- Int wrapper -------------------------------

trait StdIntWrapper {
  protected def n: Int

  /** Форматировать число, добавляя заданную строку в разделители тысячных разрядов. */
  def formatBigNumber(thousandSeparator: String): String = StdStr.bigNumber(n, thousandSeparator)
  def formatBigNumberS: String = StdStr.bigNumberS(n)
  def formatBigNumberHairSpace: String = StdStr.bigNumberHairSpace(n)

  def lpad(letters: Int, letter: Char): String =
    StringUtils.leftPad(String.valueOf(n), letters, letter)
  def zpad(zeroes: Int): String =
    StringUtils.leftPad(String.valueOf(n), zeroes, '0')

  def sizeInKb: String = Math.round(n / 1024.0).toString
  def sizeInKbNbspKb: String = sizeInKb + "&nbsp;кб"
}
