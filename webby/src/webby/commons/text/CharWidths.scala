package webby.commons.text
import com.google.common.base.CharMatcher

class CharWidths(val firstCode: Char, val lastCode: Char, val widths: Array[Int]) {
  val lastOuterCode = firstCode + widths.length
  assert(lastCode + 1 == lastOuterCode, "lastCode-firstCode and widths size does not match")

  def contains(char: Char): Boolean = char >= firstCode && char < lastOuterCode
  def getWidth(char: Char): Int = widths(char - firstCode)
}

/**
  * Класс для определения длины строки в пикселях.
  * Пример использования см. в [[StdLetterWidths]]
  */
class CharWidthUnion(val whiteSpaceWidth: Int, val defaultWidth: Int, val charWidths: Seq[CharWidths]) {
  def getWidth(char: Char): Int = {
    if (CharMatcher.whitespace().matches(char)) whiteSpaceWidth
    else charWidths.find(_.contains(char)).map(_.getWidth(char)).getOrElse(defaultWidth)
  }

  /**
    * Базовый метод для нахождения точки разрыва длинной строки.
    * По правилам html, несколько подряд идущих пробельных символов считаются за один пробел.
    *
    * @param str         Данная строка
    * @param start       Индекс начала в строке
    * @param columnWidth Ширина столбца, в которую вписывается строка
    * @return Возвращает индекс слова, перед которым следует поставить разрыв. Если строка помещается целиком, и разрывы не нужны, то возвращается None
    */
  def stringBreakFinder(str: String, columnWidth: Int, start: Int): Option[Int] = {
    var pos = 0 // позиция в строке по ширине
    var i = start // индекс текущей буквы
    var lastWordStart = start // индекс начала последнего слова
    var lastBreak = start // индекс последнего пробела, идущего сразу после буквы
    var inWhiteSpace = false
    while (i < str.length && (pos <= columnWidth || lastWordStart == start)) {
      // Условие || lastWordStart == start гарантирует, что строка, состоящая целиком из неделимого слова, не вернёт 0.
      val ch = str.charAt(i)
      if (ch == '\n') {
        return Some(i + 1)
      } else if (CharMatcher.breakingWhitespace().matches(ch)) {
        if (!inWhiteSpace) {
          // Слово закончилось - переходим к пробелам
          lastBreak = i
          inWhiteSpace = true
        }
      } else {
        if (inWhiteSpace) {
          // Пробелы закончились - начинаем слово
          pos += whiteSpaceWidth + getWidth(ch)
          lastWordStart = i
          inWhiteSpace = false
        } else {
          pos += getWidth(ch)
        }
      }
      i += 1
    }
    if (pos <= columnWidth) {
      None // Строка целиком уместилась
    } else {
      if (lastWordStart == start) None // Строка не уместилась, но она состоит из неразрывного слова
      else Some(lastWordStart) // Строка не уместилось, делаем разрыв
    }
  }

  def stringBreaks(str: String, columnWidth: Int, start: Option[Int]): Stream[Int] = start match {
    case None => Stream.empty
    case Some(st) => Stream.cons(st, stringBreaks(str, columnWidth, stringBreakFinder(str, columnWidth, st)))
  }

  /**
    * Найти точки разрыва (переноса) длинной строки.
    * Например, для строки "abc abc abc abc" и довольно малой ширине столбца метод вернёт (4,8,12).
    * Если строку не нужно разбивать (в ней нет переносов), то метод вернёт пустой Stream.
    *
    * @param str         Данная строка
    * @param columnWidth Ширина столбца, в которую вписывается строка
    */
  def stringBreaks(str: String, columnWidth: Int): Stream[Int] =
    stringBreaks(str, columnWidth, stringBreakFinder(str, columnWidth, 0))
}
