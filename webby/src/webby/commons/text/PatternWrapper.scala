package webby.commons.text

import java.io.{IOException, Reader}

import jregex._

//
// =================================== Pattern Wrapper ===================================
//
class PatternWrapper(val pat: Pattern) {

  /**
   * How many capturing groups this expression includes?
   */
  def groupCount: Int = pat.groupCount()

  /**
   * Searches through a target for a matching substring, starting from just after the end of last match.
   * If there wasn't any search performed, starts from zero.
   * @return <code>true</code> if a match found.
   */
  def find(s: String): Boolean = pat.matcher(s).find()

  /**
   * A shorthand for Pattern.matcher(String).matches().
   * @return true if the entire target matches the pattern
   */
  def matches(s: String): Boolean = pat.matcher(s).matches()

  /**
   * A shorthand for Pattern.matcher(String).matchesPrefix().<br>
   * @param s the target
   * @return true if the entire target matches the beginning of the pattern
   * @see Matcher#matchesPrefix()
   */
  def startsWith(s: String): Boolean = pat.matcher().matchesPrefix()

  /**
   * Returns a matcher for a specified string.
   */
  def matcher(s: String): Matcher = pat.matcher(s)

  /**
   * Returns a matcher for a specified region.
   */
  def matcher(data: Array[Char], start: Int, end: Int): Matcher = pat.matcher(data, start, end)

  /**
   * Returns a matcher taking a text stream as target.
   * <b>Note that this is not a true POSIX-style stream matching</b>, i.e. the whole length of the text is preliminary read and stored in a char array.
   * @param text a text stream
   * @param length the length to read from a stream; if <code>len</code> is <code>-1</code>, the whole stream is read in.
   * @throws IOException indicates an IO problem
   * @throws OutOfMemoryError if a stream is too lengthy
   */
  def matcher(text: Reader, length: Int): Matcher = pat.matcher(text, length)

  /**
   * Returns a replacer of a pattern by specified perl-like expression.
   * Such replacer will substitute all occurences of a pattern by an evaluated expression
   * ("$&" and "$0" will substitute by the whole match, "$1" will substitute by group#1, etc).
   * Example:
   * <pre>
   * String text="The quick brown fox jumped over the lazy dog";
   * Pattern word=new Pattern("\\w+");
   * System.out.println(word.replacer("[$&]").replace(text));
   * //prints "[The] [quick] [brown] [fox] [jumped] [over] [the] [lazy] [dog]"
   *
   * Pattern swap=new Pattern("(fox|dog)(.*?)(fox|dog)");
   * System.out.println(swap.replacer("$3$2$1").replace(text));
   * //prints "The quick brown dog jumped over the lazy fox"
   *
   * Pattern scramble=new Pattern("(\\w+)(.*?)(\\w+)");
   * System.out.println(scramble.replacer("$3$2$1").replace(text));
   * //prints "quick The fox brown over jumped lazy the dog"
   * </pre>
   * @param expr a perl-like expression, the "$&" and "${&}" standing for whole match, the "$N" and "${N}" standing for group#N, and "${Foo}" standing for named group Foo.
   * @see Replacer
   */
  def replacer(expr: String): Replacer = pat.replacer(expr)

  /**
   * Returns a replacer will substitute all occurences of a pattern
   * through applying a user-defined substitution model.
   * @param model a Substitution object which is in charge for match substitution
   * @see Replacer
   */
  def replacer(model: Substitution): Replacer = pat.replacer(model)

  /**
   * Заменить найденный текст на выражение expr (например, "$1") в тексте inText
   */
  def replace(expr: String, inText: String): String = pat.replacer(expr).replace(inText)

  /**
   * Tokenizes a text by an occurences of the pattern.
   * Note that a series of adjacent matches are regarded as a single separator.
   * The same as new RETokenizer(Pattern,String);
   * @see RETokenizer
   * @see RETokenizer#RETokenizer(jregex.Pattern,java.lang.String)
   *
   */
  def tokenizer(text: String): RETokenizer = pat.tokenizer(text)

  /**
   * Tokenizes a specified region by an occurences of the pattern.
   * Note that a series of adjacent matches are regarded as a single separator.
   * The same as new RETokenizer(Pattern,char[],int,int);
   * @see RETokenizer
   * @see RETokenizer#RETokenizer(jregex.Pattern,char[],int,int)
   */
  def tokenizer(data: Array[Char], off: Int, len: Int): RETokenizer = pat.tokenizer(data, off, len)

  /**
   * Tokenizes a specified region by an occurences of the pattern.
   * Note that a series of adjacent matches are regarded as a single separator.
   * The same as new RETokenizer(Pattern,Reader,int);
   * @see RETokenizer
   * @see RETokenizer#RETokenizer(jregex.Pattern,java.io.Reader,int)
   */
  def tokenizer(in: Reader, length: Int): RETokenizer = pat.tokenizer(in, length)

  override def toString: String = pat.toString

  /**
   * Returns a less or more readable representation of a bytecode for the pattern.
   */
  def toString_d: String = pat.toString_d

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
   * Сопоставляет регулярку с заданной строкой, и если начало строки совпало (matchesPrefix), то возаращается Some(Matcher), иначе None.
   */
  def mapStartsWith(s: String): Option[Matcher] = {
    val m = pat.matcher(s)
    if (m.matchesPrefix()) Some(m) else None
  }

  /**
   * Ищет регулярку в заданной строке, и если она нашлась (find), то возаращается Some(Matcher), иначе None.
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
    m.setPosition(startIdx)
    if (m.find()) Some(m) else None
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
    if (m.matches()) Some(m.groups().toSeq.drop(1))
    else None
  }

  def findAll(s: String): scala.Iterator[MatchResult] = {
    val m = pat.matcher(s)
    val it = m.findAll()
    new scala.Iterator[MatchResult] {
      def hasNext: Boolean = it.hasMore
      def next(): MatchResult = it.nextMatch()
    }
  }
}
