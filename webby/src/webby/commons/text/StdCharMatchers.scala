package webby.commons.text
import com.google.common.base.CharMatcher

object StdCharMatchers {
  object rusLetters extends CharMatcher {
    override def matches(c: Char): Boolean = (c >= 'а' && c <= 'я') || (c >= 'А' && c <= 'Я') || c == 'ё' || c == 'Ё'
  }
  object engLetters extends  CharMatcher {
    override def matches(c: Char): Boolean = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
  }
  object whiteSpaceAndHyphen extends CharMatcher {
    override def matches(c: Char): Boolean = c == '-' || Character.isWhitespace(c)
  }
  object htmlChars extends CharMatcher {
    override def matches(c: Char): Boolean = c == '<' || c == '>'
  }
  /** Символы, которые можно вырезать в конце строки, чтобы поставить многоточие */
  object endingSymbols extends CharMatcher {
    override def matches(c: Char): Boolean = c == '.' || c == ',' || c == '‚' || c == ':' ||
      c == ';' || c == '-' || c == '!' || c == '?' || Character.isWhitespace(c)
  }
  /** Символы (не буквы), которые могут быть использованы в именах */
  object nameSymbols extends CharMatcher {
    override def matches(c: Char): Boolean = c == '.' || c == '-' || c == '\''
  }

  val rusLettersDigits = rusLetters.or(CharMatcher.DIGIT)

  object passwordSymbols extends CharMatcher {
    override def matches(c: Char): Boolean = c match {
      case ' ' | '`' | '~' | '!' | '@' | '#' | '$' | '%' | '^' | '&' | '*' | '(' | ')' | '-' | '_' | '=' | '+' |
           '[' | ']' | '{' | '}' | '\\' | '|' | ';' | ':' | '\'' | '"' | ',' | '.' | '<' | '>' | '/' | '?' => true
      case _ => false
    }
  }
  val passwordMatcher = engLetters.or(CharMatcher.DIGIT).or(passwordSymbols)

  /** Все минусы, тире, дефисы и прочие чёрточки */
  object allDashes extends CharMatcher {
    override def matches(c: Char): Boolean = c == '-' || c == '−' || c == '‒' || c == '–' || c == '—' || c == '―'
  }
}
