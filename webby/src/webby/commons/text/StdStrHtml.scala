package webby.commons.text
import com.google.common.base.CharMatcher
import org.apache.commons.lang3.{StringEscapeUtils, StringUtils}

import scala.annotation.tailrec

/**
  * Методы для работы с html в строках.
  */
trait StdStrHtml {
  /**
    * Строка имеет html теги с символами '<' или '>'?
    */
  def hasHtmlTags(str: String): Boolean = StdCharMatchers.htmlChars.matchesAnyOf(str)

  /**
    * Преобразовать html entities типа &amp;ndash; => -, и заменить опасные '<', '>' на '[' и ']' соответственно.
    * Html-кода в выходной строке не должно быть.
    * Применяется для обработки ввода юзера. Html теги не удаляет, а делает их видимыми.
    * Также, удаляются множественные пробелы внутри текста.
    */
  def unescapeAndCleanHtmlEntities(str: String): String =
    StringUtils.replaceChars(StringEscapeUtils.unescapeHtml4(str), "<>", "[]")

  /**
    * Заменяет все символы whitespace пробелами.
    */
  def cleanWhitespace(str: String): String = CharMatcher.WHITESPACE.replaceFrom(str, ' ')

  /**
    * Удаляет все символы whitespace в начале и в конце строки.
    */
  def trimWhitespace(str: String): String = CharMatcher.WHITESPACE.trimFrom(str)

  /**
    * Добавляет тег <br/> ко всем переносам строк (\n).
    */
  def addBrs(s: String): String = StringUtils.replace(s, "\n", "<br/>\n")

  /**
    * Подготавливает строку для вывода в виде html, и добавляет тег <br/> ко всем переносам строк (\n).
    */
  def escapeAddBrs(s: String): String = addBrs(StringEscapeUtils.escapeHtml4(s))

  /**
    * Специальная разновидность capitalize, поддерживающая html теги в начале строки.
    */
  def htmlCapitalize(str: String): String = {
    if (str == null) null
    else if (str.length == 0) ""
    else {
      @inline
      def capitalize(i: Int): String = {
        val chars: Array[Char] = str.toCharArray
        chars(i) = chars(i).toUpper
        new String(chars)
      }

      @tailrec
      def check(i: Int): String = {
        val c = str.charAt(i)
        if (c == '<') {
          str.indexOf('>', i + 1) match {
            case -1 => str
            case idx => if (idx + 1 < str.length) check(idx + 1) else str
          }
        } else if (c.isUpper) str else capitalize(i)
      }
      check(0)
    }
  }
}

object StdStrHtml extends StdStrHtml

trait StdStrHtmlWrapper {
  protected def s: String

  /** Строка имеет html теги с символами '<' или '>'? */
  def hasHtmlTags: Boolean = StdStrHtml.hasHtmlTags(s)

  /**
    * Преобразовать html entities типа &amp;ndash; => -, и заменить опасные '<', '>' на '[' и ']' соответственно.
    * Html-кода в выходной строке не должно быть.
    * Применяется для обработки ввода юзера. Html теги не удаляет, а делает их видимыми.
    * Также, удаляются множественные пробелы внутри текста.
    */
  def unescapeAndCleanHtmlEntities: String = StdStrHtml.unescapeAndCleanHtmlEntities(s)

  /** Заменяет все символы whitespace пробелами. */
  def cleanWhitespace: String = StdStrHtml.cleanWhitespace(s)

  /** Удаляет все символы whitespace в начале и в конце строки. */
  def trimWhitespace: String = StdStrHtml.trimWhitespace(s)

  /** Добавляет тег <br/> ко всем переносам строк (\n). */
  def addBrs: String = StdStrHtml.addBrs(s)

  /** Подготавливает строку для вывода в виде html, и добавляет тег <br/> ко всем переносам строк (\n). */
  def escapeAddBrs: String = StdStrHtml.escapeAddBrs(s)

  /** Специальная разновидность capitalize, поддерживающая html теги в начале строки. */
  def htmlCapitalize: String = StdStrHtml.htmlCapitalize(s)
}
