package webby.commons.text
import java.io.{File, PrintWriter}

import jregex.{Matcher, Pattern, REFlags}
import org.apache.commons.lang3.{StringEscapeUtils, StringUtils => SU}
import webby.commons.io.UrlEncoder
import webby.commons.time.StdDateFmt

object StringWrapper {
  import scala.language.implicitConversions

  implicit def wrapper(s: String): StringWrapper = new StringWrapper(s)
  implicit def wrapper(c: Char): CharWrapper = new CharWrapper(c)
  implicit def wrapper(pat: Pattern): PatternWrapper = new PatternWrapper(pat)

  //
  // =================================== String Wrapper ===================================
  //
  class StringWrapper(s: String) {

    /** Checks if a CharSequence is empty ("") or null. */
    def isEmptyNull: Boolean = SU.isEmpty(s)
    /** Checks if a CharSequence is not empty ("") and not null. */
    def isNotEmpty: Boolean = SU.isNotEmpty(s)

    /** Checks if a CharSequence is whitespace, empty ("") or null. */
    def isBlank: Boolean = SU.isBlank(s)
    /** Checks if a CharSequence is not empty (""), not null and not whitespace only. */
    def isNotBlank: Boolean = SU.isNotBlank(s)

    /**
      * Removes control characters (char <= 32) from both ends of this String returning null
      * if the String is empty ("") after the trim or if it is null.
      */
    def trimToNull: String = SU.trimToNull(s)

    /**
      * Removes control characters (char <= 32) from both
      * ends of this String returning an empty String ("") if the String
      * is empty ("") after the trim or if it is null
      */
    def trimToEmpty: String = SU.trimToEmpty(s)

    /**
      * Removes control characters (char <= 32) from both ends of this String returning None
      * if the String is empty ("") after the trim or if it is null.
      */
    def trimToOption: Option[String] = {
      val ts = SU.trim(s)
      if (SU.isEmpty(ts)) None else Some(ts)
    }

    @inline def strFold[B](ifEmpty: => B)(f: String => B): B = if (SU.isEmpty(s)) ifEmpty else f(s)

    /**
      * Делает trim строки, и применяет к ней fold:
      * Если строка пустая, то вызывается ifEmpty.
      * Если не пустая, то происходит трансформация через функцию f.
      */
    @inline def trimToFold[B](ifEmpty: => B)(f: String => B): B = {
      val ts = SU.trim(s)
      if (SU.isEmpty(ts)) ifEmpty else f(ts)
    }

    /**
      * Делает trim строки, и возвращает ifEmpty, если строка пустая, иначе возвращает саму строку.
      */
    @inline def trimOr(ifEmpty: => String): String = {
      val ts = SU.trim(s)
      if (SU.isEmpty(ts)) ifEmpty else ts
    }

    @inline def or(ifEmpty: => String): String = if (SU.isEmpty(s)) ifEmpty else s

    /** Returns first #chars characters. Simple wrapper around String#substring but without StringIndexOutOfBoundsException. */
    def takeFirst(chars: Int) = if (s.length <= chars) s else s.substring(0, chars)

    // Splitting
    //-----------------------------------------------------------------------
    /**
      * <p>Splits the provided text into an array, using whitespace as the
      * separator.
      * Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
      *
      * <p>The separator is not included in the returned String array.
      * Adjacent separators are treated as one separator.
      * For more control over the split use the StrTokenizer class.</p>
      *
      * <p>A {@code null} input String returns {@code null}.</p>
      *
      * <pre>
      * StringUtils.split(null)       = null
      * StringUtils.split("")         = []
      * StringUtils.split("abc def")  = ["abc", "def"]
      * StringUtils.split("abc  def") = ["abc", "def"]
      * StringUtils.split(" abc ")    = ["abc"]
      * </pre>
      *
      * s:  the String to parse, may be null
      * @return an array of parsed Strings, { @code null} if null String input
      */
    def splitStd: Array[String] = SU.split(s)

    /**
      * <p>Splits the provided text into an array, separator specified.
      * This is an alternative to using StringTokenizer.</p>
      *
      * <p>The separator is not included in the returned String array.
      * Adjacent separators are treated as one separator.
      * For more control over the split use the StrTokenizer class.</p>
      *
      * <p>A {@code null} input String returns {@code null}.</p>
      *
      * <pre>
      * StringUtils.split(null, *)         = null
      * StringUtils.split("", *)           = []
      * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
      * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
      * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
      * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
      * </pre>
      *
      * s:  the String to parse, may be null
      * @param separatorChar the character used as the delimiter
      * @return an array of parsed Strings, { @code null} if null String input
      * @since 2.0
      */
    def splitChar(separatorChar: Char): Array[String] = SU.split(s, separatorChar)

    /**
      * <p>Splits the provided text into an array, separators specified.
      * This is an alternative to using StringTokenizer.</p>
      *
      * <p>The separator is not included in the returned String array.
      * Adjacent separators are treated as one separator.
      * For more control over the split use the StrTokenizer class.</p>
      *
      * <p>A {@code null} input String returns {@code null}.
      * A {@code null} separatorChars splits on whitespace.</p>
      *
      * <pre>
      * StringUtils.split(null, *)         = null
      * StringUtils.split("", *)           = []
      * StringUtils.split("abc def", null) = ["abc", "def"]
      * StringUtils.split("abc def", " ")  = ["abc", "def"]
      * StringUtils.split("abc  def", " ") = ["abc", "def"]
      * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
      * </pre>
      *
      * s:  the String to parse, may be null
      * @param separatorChars the characters used as the delimiters,
      *                       { @code null} splits on whitespace
      * @return an array of parsed Strings, { @code null} if null String input
      */
    def splitChars(separatorChars: String): Array[String] = SU.split(s, separatorChars)

    /**
      * <p>Splits the provided text into an array with a maximum length,
      * separators specified.</p>
      *
      * <p>The separator is not included in the returned String array.
      * Adjacent separators are treated as one separator.</p>
      *
      * <p>A {@code null} input String returns {@code null}.
      * A {@code null} separatorChars splits on whitespace.</p>
      *
      * <p>If more than {@code max} delimited substrings are found, the last
      * returned string includes all characters after the first {@code max - 1}
      * returned strings (including separator characters).</p>
      *
      * <pre>
      * StringUtils.split(null, *, *)            = null
      * StringUtils.split("", *, *)              = []
      * StringUtils.split("ab de fg", null, 0)   = ["ab", "cd", "ef"]
      * StringUtils.split("ab   de fg", null, 0) = ["ab", "cd", "ef"]
      * StringUtils.split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
      * StringUtils.split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
      * </pre>
      *
      * s:  the String to parse, may be null
      * @param separatorChars the characters used as the delimiters,
      *                       { @code null} splits on whitespace
      * @param max            the maximum number of elements to include in the
      *                       array. A zero or negative value implies no limit
      * @return an array of parsed Strings, { @code null} if null String input
      */
    def splitChars(separatorChars: String, max: Int) = SU.split(s, separatorChars, max)

    /**
      * <p>Splits the provided text into an array, separator string specified.</p>
      *
      * <p>The separator(s) will not be included in the returned String array.
      * Adjacent separators are treated as one separator.</p>
      *
      * <p>A {@code null} input String returns {@code null}.
      * A {@code null} separator splits on whitespace.</p>
      *
      * <pre>
      * StringUtils.splitByWholeSeparator(null, *)               = null
      * StringUtils.splitByWholeSeparator("", *)                 = []
      * StringUtils.splitByWholeSeparator("ab de fg", null)      = ["ab", "de", "fg"]
      * StringUtils.splitByWholeSeparator("ab   de fg", null)    = ["ab", "de", "fg"]
      * StringUtils.splitByWholeSeparator("ab:cd:ef", ":")       = ["ab", "cd", "ef"]
      * StringUtils.splitByWholeSeparator("ab-!-cd-!-ef", "-!-") = ["ab", "cd", "ef"]
      * </pre>
      *
      * @param separator String containing the String to be used as a delimiter,
      *                  { @code null} splits on whitespace
      * @return an array of parsed Strings, { @code null} if null String was input
      */
    def splitByWholeSeparator(separator: String): Array[String] =
      SU.splitByWholeSeparator(s, separator)

    /**
      * <p>Splits the provided text into an array, separator string specified.
      * Returns a maximum of {@code max} substrings.</p>
      *
      * <p>The separator(s) will not be included in the returned String array.
      * Adjacent separators are treated as one separator.</p>
      *
      * <p>A {@code null} input String returns {@code null}.
      * A {@code null} separator splits on whitespace.</p>
      *
      * <pre>
      * StringUtils.splitByWholeSeparator(null, *, *)               = null
      * StringUtils.splitByWholeSeparator("", *, *)                 = []
      * StringUtils.splitByWholeSeparator("ab de fg", null, 0)      = ["ab", "de", "fg"]
      * StringUtils.splitByWholeSeparator("ab   de fg", null, 0)    = ["ab", "de", "fg"]
      * StringUtils.splitByWholeSeparator("ab:cd:ef", ":", 2)       = ["ab", "cd:ef"]
      * StringUtils.splitByWholeSeparator("ab-!-cd-!-ef", "-!-", 5) = ["ab", "cd", "ef"]
      * StringUtils.splitByWholeSeparator("ab-!-cd-!-ef", "-!-", 2) = ["ab", "cd-!-ef"]
      * </pre>
      *
      * @param separator String containing the String to be used as a delimiter,
      *                  { @code null} splits on whitespace
      * @param max       the maximum number of elements to include in the returned
      *                  array. A zero or negative value implies no limit.
      * @return an array of parsed Strings, { @code null} if null String was input
      */
    def splitByWholeSeparator(separator: String, max: Int): Array[String] =
      SU.splitByWholeSeparator(s, separator, max)

    /**
      * <p>Splits the provided text into an array, separator string specified. </p>
      *
      * <p>The separator is not included in the returned String array.
      * Adjacent separators are treated as separators for empty tokens.
      * For more control over the split use the StrTokenizer class.</p>
      *
      * <p>A {@code null} input String returns {@code null}.
      * A {@code null} separator splits on whitespace.</p>
      *
      * <pre>
      * StringUtils.splitByWholeSeparatorPreserveAllTokens(null, *)               = null
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("", *)                 = []
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab de fg", null)      = ["ab", "de", "fg"]
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab   de fg", null)    = ["ab", "", "", "de", "fg"]
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab:cd:ef", ":")       = ["ab", "cd", "ef"]
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab-!-cd-!-ef", "-!-") = ["ab", "cd", "ef"]
      * </pre>
      *
      * @param separator String containing the String to be used as a delimiter,
      *                  { @code null} splits on whitespace
      * @return an array of parsed Strings, { @code null} if null String was input
      * @since 2.4
      */
    def splitByWholeSeparatorPreserveAllTokens(separator: String): Array[String] =
      SU.splitByWholeSeparatorPreserveAllTokens(s, separator)

    /**
      * <p>Splits the provided text into an array, separator string specified.
      * Returns a maximum of {@code max} substrings.</p>
      *
      * <p>The separator is not included in the returned String array.
      * Adjacent separators are treated as separators for empty tokens.
      * For more control over the split use the StrTokenizer class.</p>
      *
      * <p>A {@code null} input String returns {@code null}.
      * A {@code null} separator splits on whitespace.</p>
      *
      * <pre>
      * StringUtils.splitByWholeSeparatorPreserveAllTokens(null, *, *)               = null
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("", *, *)                 = []
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab de fg", null, 0)      = ["ab", "de", "fg"]
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab   de fg", null, 0)    = ["ab", "", "", "de", "fg"]
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab:cd:ef", ":", 2)       = ["ab", "cd:ef"]
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab-!-cd-!-ef", "-!-", 5) = ["ab", "cd", "ef"]
      * StringUtils.splitByWholeSeparatorPreserveAllTokens("ab-!-cd-!-ef", "-!-", 2) = ["ab", "cd-!-ef"]
      * </pre>
      *
      * @param separator String containing the String to be used as a delimiter,
      *                  { @code null} splits on whitespace
      * @param max       the maximum number of elements to include in the returned
      *                  array. A zero or negative value implies no limit.
      * @return an array of parsed Strings, { @code null} if null String was input
      * @since 2.4
      */
    def splitByWholeSeparatorPreserveAllTokens(separator: String, max: Int): Array[String] =
      SU.splitByWholeSeparatorPreserveAllTokens(s, separator, max)

    // Replacing
    //-----------------------------------------------------------------------

    /**
      * <p>Replaces a String with another String inside a larger String, once.</p>
      *
      * <p>A {@code null} reference passed to this method is a no-op.</p>
      *
      * <pre>
      * StringUtils.replaceOnce(null, *, *)        = null
      * StringUtils.replaceOnce("", *, *)          = ""
      * StringUtils.replaceOnce("any", null, *)    = "any"
      * StringUtils.replaceOnce("any", *, null)    = "any"
      * StringUtils.replaceOnce("any", "", *)      = "any"
      * StringUtils.replaceOnce("aba", "a", null)  = "aba"
      * StringUtils.replaceOnce("aba", "a", "")    = "ba"
      * StringUtils.replaceOnce("aba", "a", "z")   = "zba"
      * </pre>
      *
      * @see #replace(String text, String searchString, String replacement, int max)
      * @param searchString the String to search for, may be null
      * @param replacement  the String to replace with, may be null
      * @return the text with any replacements processed,
      *         { @code null} if null String input
      */
    def replaceOnce(searchString: String, replacement: String): String = SU.replaceOnce(s, searchString, replacement)

    /**
      * <p>Replaces all occurrences of a String within another String.</p>
      *
      * <p>A {@code null} reference passed to this method is a no-op.</p>
      *
      * <pre>
      * StringUtils.replace(null, *, *)        = null
      * StringUtils.replace("", *, *)          = ""
      * StringUtils.replace("any", null, *)    = "any"
      * StringUtils.replace("any", *, null)    = "any"
      * StringUtils.replace("any", "", *)      = "any"
      * StringUtils.replace("aba", "a", null)  = "aba"
      * StringUtils.replace("aba", "a", "")    = "b"
      * StringUtils.replace("aba", "a", "z")   = "zbz"
      * </pre>
      *
      * @see #replace(String text, String searchString, String replacement, int max)
      * @param searchString the String to search for, may be null
      * @param replacement  the String to replace it with, may be null
      * @return the text with any replacements processed,
      *         { @code null} if null String input
      */
    def replaceStd(searchString: String, replacement: String): String = SU.replace(s, searchString, replacement)

    /**
      * <p>Replaces a String with another String inside a larger String,
      * for the first {@code max} values of the search String.</p>
      *
      * <p>A {@code null} reference passed to this method is a no-op.</p>
      *
      * <pre>
      * StringUtils.replace(null, *, *, *)         = null
      * StringUtils.replace("", *, *, *)           = ""
      * StringUtils.replace("any", null, *, *)     = "any"
      * StringUtils.replace("any", *, null, *)     = "any"
      * StringUtils.replace("any", "", *, *)       = "any"
      * StringUtils.replace("any", *, *, 0)        = "any"
      * StringUtils.replace("abaa", "a", null, -1) = "abaa"
      * StringUtils.replace("abaa", "a", "", -1)   = "b"
      * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
      * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
      * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
      * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
      * </pre>
      *
      * @param searchString the String to search for, may be null
      * @param replacement  the String to replace it with, may be null
      * @param max          maximum number of values to replace, or { @code -1} if no maximum
      * @return the text with any replacements processed,
      *         { @code null} if null String input
      */
    def replaceStd(searchString: String, replacement: String, max: Int): String = SU.replace(s, searchString, replacement, max)

    /**
      * <p>Replaces multiple characters in a String in one go.
      * This method can also be used to delete characters.</p>
      *
      * <p>For example:<br />
      * <code>replaceChars(&quot;hello&quot;, &quot;ho&quot;, &quot;jy&quot;) = jelly</code>.</p>
      *
      * <p>A {@code null} string input returns {@code null}.
      * An empty ("") string input returns an empty string.
      * A null or empty set of search characters returns the input string.</p>
      *
      * <p>The length of the search characters should normally equal the length
      * of the replace characters.
      * If the search characters is longer, then the extra search characters
      * are deleted.
      * If the search characters is shorter, then the extra replace characters
      * are ignored.</p>
      *
      * <pre>
      * StringUtils.replaceChars(null, *, *)           = null
      * StringUtils.replaceChars("", *, *)             = ""
      * StringUtils.replaceChars("abc", null, *)       = "abc"
      * StringUtils.replaceChars("abc", "", *)         = "abc"
      * StringUtils.replaceChars("abc", "b", null)     = "ac"
      * StringUtils.replaceChars("abc", "b", "")       = "ac"
      * StringUtils.replaceChars("abcba", "bc", "yz")  = "ayzya"
      * StringUtils.replaceChars("abcba", "bc", "y")   = "ayya"
      * StringUtils.replaceChars("abcba", "bc", "yzx") = "ayzya"
      * </pre>
      *
      * #s:  String to replace characters in, may be null
      * @param searchChars  a set of characters to search for, may be null
      * @param replaceChars a set of characters to replace, may be null
      * @return modified String, { @code null} if null string input
      * @since 2.0
      */
    def replaceChars(searchChars: String, replaceChars: String) = SU.replaceChars(s, searchChars, replaceChars)

    // Padding
    //-----------------------------------------------------------------------

    /**
      * <p>Repeat a String {@code repeat} times to form a
      * new String.</p>
      *
      * <pre>
      * StringUtils.repeat(null, 2) = null
      * StringUtils.repeat("", 0)   = ""
      * StringUtils.repeat("", 2)   = ""
      * StringUtils.repeat("a", 3)  = "aaa"
      * StringUtils.repeat("ab", 2) = "abab"
      * StringUtils.repeat("a", -2) = ""
      * </pre>
      *
      * s: the String to repeat, may be null
      * @param repeat number of times to repeat str, negative treated as zero
      * @return a new String consisting of the original String repeated,
      *         { @code null} if null String input
      */
    def repeat(repeat: Int): String = SU.repeat(s, repeat)

    /**
      * <p>Repeat a String {@code repeat} times to form a
      * new String, with a String separator injected each time. </p>
      *
      * <pre>
      * StringUtils.repeat(null, null, 2) = null
      * StringUtils.repeat(null, "x", 2)  = null
      * StringUtils.repeat("", null, 0)   = ""
      * StringUtils.repeat("", "", 2)     = ""
      * StringUtils.repeat("", "x", 3)    = "xxx"
      * StringUtils.repeat("?", ", ", 3)  = "?, ?, ?"
      * </pre>
      *
      * s:        the String to repeat, may be null
      * @param separator the String to inject, may be null
      * @param repeat    number of times to repeat str, negative treated as zero
      * @return a new String consisting of the original String repeated,
      *         { @code null} if null String input
      * @since 2.5
      */
    def repeat(separator: String, repeat: Int): String = SU.repeat(s, separator, repeat)

    /**
      * <p>Right pad a String with spaces (' ').</p>
      *
      * <p>The String is padded to the size of {@code size}.</p>
      *
      * <pre>
      * StringUtils.rightPad(null, *)   = null
      * StringUtils.rightPad("", 3)     = "   "
      * StringUtils.rightPad("bat", 3)  = "bat"
      * StringUtils.rightPad("bat", 5)  = "bat  "
      * StringUtils.rightPad("bat", 1)  = "bat"
      * StringUtils.rightPad("bat", -1) = "bat"
      * </pre>
      *
      * s: the String to pad out, may be null
      * @param size the size to pad to
      * @return right padded String or original String if no padding is necessary,
      *         { @code null} if null String input
      */
    def rightPad(size: Int): String = SU.rightPad(s, size)

    /**
      * <p>Right pad a String with a specified character.</p>
      *
      * <p>The String is padded to the size of {@code size}.</p>
      *
      * <pre>
      * StringUtils.rightPad(null, *, *)     = null
      * StringUtils.rightPad("", 3, 'z')     = "zzz"
      * StringUtils.rightPad("bat", 3, 'z')  = "bat"
      * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
      * StringUtils.rightPad("bat", 1, 'z')  = "bat"
      * StringUtils.rightPad("bat", -1, 'z') = "bat"
      * </pre>
      *
      * s:   the String to pad out, may be null
      * @param size    the size to pad to
      * @param padChar the character to pad with
      * @return right padded String or original String if no padding is necessary,
      *         { @code null} if null String input
      * @since 2.0
      */
    def rightPad(size: Int, padChar: Char): String = SU.rightPad(s, size, padChar)

    /**
      * <p>Right pad a String with a specified String.</p>
      *
      * <p>The String is padded to the size of {@code size}.</p>
      *
      * <pre>
      * StringUtils.rightPad(null, *, *)      = null
      * StringUtils.rightPad("", 3, "z")      = "zzz"
      * StringUtils.rightPad("bat", 3, "yz")  = "bat"
      * StringUtils.rightPad("bat", 5, "yz")  = "batyz"
      * StringUtils.rightPad("bat", 8, "yz")  = "batyzyzy"
      * StringUtils.rightPad("bat", 1, "yz")  = "bat"
      * StringUtils.rightPad("bat", -1, "yz") = "bat"
      * StringUtils.rightPad("bat", 5, null)  = "bat  "
      * StringUtils.rightPad("bat", 5, "")    = "bat  "
      * </pre>
      *
      * s:  the String to pad out, may be null
      * @param size   the size to pad to
      * @param padStr the String to pad with, null or empty treated as single space
      * @return right padded String or original String if no padding is necessary,
      *         { @code null} if null String input
      */
    def rightPad(size: Int, padStr: String): String = SU.rightPad(s, size, padStr)

    /**
      * <p>Left pad a String with spaces (' ').</p>
      *
      * <p>The String is padded to the size of {@code size}.</p>
      *
      * <pre>
      * StringUtils.leftPad(null, *)   = null
      * StringUtils.leftPad("", 3)     = "   "
      * StringUtils.leftPad("bat", 3)  = "bat"
      * StringUtils.leftPad("bat", 5)  = "  bat"
      * StringUtils.leftPad("bat", 1)  = "bat"
      * StringUtils.leftPad("bat", -1) = "bat"
      * </pre>
      *
      * s: the String to pad out, may be null
      * @param size the size to pad to
      * @return left padded String or original String if no padding is necessary,
      *         { @code null} if null String input
      */
    def leftPad(size: Int): String = SU.leftPad(s, size)

    /**
      * <p>Left pad a String with a specified character.</p>
      *
      * <p>Pad to a size of {@code size}.</p>
      *
      * <pre>
      * StringUtils.leftPad(null, *, *)     = null
      * StringUtils.leftPad("", 3, 'z')     = "zzz"
      * StringUtils.leftPad("bat", 3, 'z')  = "bat"
      * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
      * StringUtils.leftPad("bat", 1, 'z')  = "bat"
      * StringUtils.leftPad("bat", -1, 'z') = "bat"
      * </pre>
      *
      * s: the String to pad out, may be null
      * @param size    the size to pad to
      * @param padChar the character to pad with
      * @return left padded String or original String if no padding is necessary,
      *         { @code null} if null String input
      * @since 2.0
      */
    def leftPad(size: Int, padChar: Char): String = SU.leftPad(s, size, padChar)

    /**
      * <p>Left pad a String with a specified String.</p>
      *
      * <p>Pad to a size of {@code size}.</p>
      *
      * <pre>
      * StringUtils.leftPad(null, *, *)      = null
      * StringUtils.leftPad("", 3, "z")      = "zzz"
      * StringUtils.leftPad("bat", 3, "yz")  = "bat"
      * StringUtils.leftPad("bat", 5, "yz")  = "yzbat"
      * StringUtils.leftPad("bat", 8, "yz")  = "yzyzybat"
      * StringUtils.leftPad("bat", 1, "yz")  = "bat"
      * StringUtils.leftPad("bat", -1, "yz") = "bat"
      * StringUtils.leftPad("bat", 5, null)  = "  bat"
      * StringUtils.leftPad("bat", 5, "")    = "  bat"
      * </pre>
      *
      * s: the String to pad out, may be null
      * @param size   the size to pad to
      * @param padStr the String to pad with, null or empty treated as single space
      * @return left padded String or original String if no padding is necessary,
      *         { @code null} if null String input
      */
    def leftPad(size: Int, padStr: String): String = SU.leftPad(s, size, padStr)

    /**
      * Left pad with zeros.
      */
    def zPad(size: Int): String = SU.leftPad(s, size, '0')

    // Centering
    //-----------------------------------------------------------------------
    /**
      * <p>Centers a String in a larger String of size {@code size}
      * using the space character (' ').<p>
      *
      * <p>If the size is less than the String length, the String is returned.
      * A {@code null} String returns {@code null}.
      * A negative size is treated as zero.</p>
      *
      * <p>Equivalent to {@code center(str, size, " ")}.</p>
      *
      * <pre>
      * StringUtils.center(null, *)   = null
      * StringUtils.center("", 4)     = "    "
      * StringUtils.center("ab", -1)  = "ab"
      * StringUtils.center("ab", 4)   = " ab "
      * StringUtils.center("abcd", 2) = "abcd"
      * StringUtils.center("a", 4)    = " a  "
      * </pre>
      *
      * s: the String to center, may be null
      * @param size the int size of new String, negative treated as zero
      * @return centered String, { @code null} if null String input
      */
    def center(size: Int): String = SU.center(s, size)

    /**
      * <p>Centers a String in a larger String of size {@code size}.
      * Uses a supplied character as the value to pad the String with.</p>
      *
      * <p>If the size is less than the String length, the String is returned.
      * A {@code null} String returns {@code null}.
      * A negative size is treated as zero.</p>
      *
      * <pre>
      * StringUtils.center(null, *, *)     = null
      * StringUtils.center("", 4, ' ')     = "    "
      * StringUtils.center("ab", -1, ' ')  = "ab"
      * StringUtils.center("ab", 4, ' ')   = " ab"
      * StringUtils.center("abcd", 2, ' ') = "abcd"
      * StringUtils.center("a", 4, ' ')    = " a  "
      * StringUtils.center("a", 4, 'y')    = "yayy"
      * </pre>
      *
      * s: the String to center, may be null
      * @param size    the int size of new String, negative treated as zero
      * @param padChar the character to pad the new String with
      * @return centered String, { @code null} if null String input
      * @since 2.0
      */
    def center(size: Int, padChar: Char): String = SU.center(s, size, padChar)

    /**
      * <p>Centers a String in a larger String of size {@code size}.
      * Uses a supplied String as the value to pad the String with.</p>
      *
      * <p>If the size is less than the String length, the String is returned.
      * A {@code null} String returns {@code null}.
      * A negative size is treated as zero.</p>
      *
      * <pre>
      * StringUtils.center(null, *, *)     = null
      * StringUtils.center("", 4, " ")     = "    "
      * StringUtils.center("ab", -1, " ")  = "ab"
      * StringUtils.center("ab", 4, " ")   = " ab"
      * StringUtils.center("abcd", 2, " ") = "abcd"
      * StringUtils.center("a", 4, " ")    = " a  "
      * StringUtils.center("a", 4, "yz")   = "yayz"
      * StringUtils.center("abc", 7, null) = "  abc  "
      * StringUtils.center("abc", 7, "")   = "  abc  "
      * </pre>
      *
      * s: the String to center, may be null
      * @param size   the int size of new String, negative treated as zero
      * @param padStr the String to pad the new String with, must not be null or empty
      * @return centered String, { @code null} if null String input
      * @throws IllegalArgumentException if padStr is { @code null} or empty
      */
    def center(size: Int, padStr: String): String = SU.center(s, size, padStr)

    /**
      * <p>Checks if CharSequence contains a search CharSequence irrespective of case,
      * handling {@code null}. Case-insensitivity is defined as by
      * {@link String#equalsIgnoreCase(String)}.
      *
      * <p>A {@code null} CharSequence will return {@code false}.</p>
      *
      * <pre>
      * StringUtils.contains(null, *) = false
      * StringUtils.contains(*, null) = false
      * StringUtils.contains("", "") = true
      * StringUtils.contains("abc", "") = true
      * StringUtils.contains("abc", "a") = true
      * StringUtils.contains("abc", "z") = false
      * StringUtils.contains("abc", "A") = true
      * StringUtils.contains("abc", "Z") = false
      * </pre>
      *
      * @param searchStr the CharSequence to find, may be null
      * @return true if the CharSequence contains the search CharSequence irrespective of
      *         case or false if not or { @code null} string input
      * @since 3.0 Changed signature from containsIgnoreCase(String, String) to containsIgnoreCase(CharSequence, CharSequence)
      */
    def containsIgnoreCase(searchStr: CharSequence): Boolean = SU.containsIgnoreCase(s, searchStr)

    /**
      * <p>Checks if the CharSequence contains any character in the given
      * set of characters.</p>
      *
      * <p>A {@code null} CharSequence will return {@code false}.
      * A {@code null} or zero length search array will return {@code false}.</p>
      *
      * <pre>
      * StringUtils.containsAny(null, *)                = false
      * StringUtils.containsAny("", *)                  = false
      * StringUtils.containsAny(*, null)                = false
      * StringUtils.containsAny(*, [])                  = false
      * StringUtils.containsAny("zzabyycdxx",['z','a']) = true
      * StringUtils.containsAny("zzabyycdxx",['b','y']) = true
      * StringUtils.containsAny("aba", ['z'])           = false
      * </pre>
      *
      * @param searchChars the chars to search for, may be null
      * @return the { @code true} if any of the chars are found,
      *         { @code false} if no match or null input
      * @since 2.4
      * @since 3.0 Changed signature from containsAny(String, char[]) to containsAny(CharSequence, char...)
      */
    def containsAny(searchChars: Char*): Boolean = SU.containsAny(s, searchChars: _*)

    // ------------------------------ Substring ------------------------------

    /**
      * <p>Gets a substring from the specified String avoiding exceptions.</p>
      *
      * <p>A negative start position can be used to start {@code n}
      * characters from the end of the String.</p>
      *
      * <p>A {@code null} String will return {@code null}.
      * An empty ("") String will return "".</p>
      *
      * <pre>
      * StringUtils.substring(null, *)   = null
      * StringUtils.substring("", *)     = ""
      * StringUtils.substring("abc", 0)  = "abc"
      * StringUtils.substring("abc", 2)  = "c"
      * StringUtils.substring("abc", 4)  = ""
      * StringUtils.substring("abc", -2) = "bc"
      * StringUtils.substring("abc", -4) = "abc"
      * </pre>
      *
      * @param start the position to start from, negative means
      *              count back from the end of the String by this many characters
      * @return substring from start position, { @code null} if null String input
      */
    def substr(start: Int): String = SU.substring(s, start)

    /**
      * <p>Gets a substring from the specified String avoiding exceptions.</p>
      *
      * <p>A negative start position can be used to start/end {@code n}
      * characters from the end of the String.</p>
      *
      * <p>The returned substring starts with the character in the {@code start}
      * position and ends before the {@code end} position. All position counting is
      * zero-based -- i.e., to start at the beginning of the string use
      * {@code start = 0}. Negative start and end positions can be used to
      * specify offsets relative to the end of the String.</p>
      *
      * <p>If {@code start} is not strictly to the left of {@code end}, ""
      * is returned.</p>
      *
      * <pre>
      * StringUtils.substring(null, *, *)    = null
      * StringUtils.substring("", * ,  *)    = "";
      * StringUtils.substring("abc", 0, 2)   = "ab"
      * StringUtils.substring("abc", 2, 0)   = ""
      * StringUtils.substring("abc", 2, 4)   = "c"
      * StringUtils.substring("abc", 4, 6)   = ""
      * StringUtils.substring("abc", 2, 2)   = ""
      * StringUtils.substring("abc", -2, -1) = "b"
      * StringUtils.substring("abc", -4, 2)  = "ab"
      * </pre>
      *
      * @param start the position to start from, negative means
      *              count back from the end of the String by this many characters
      * @param end   the position to end at (exclusive), negative means
      *              count back from the end of the String by this many characters
      * @return substring from start position to end position,
      *         { @code null} if null String input
      */
    def substr(start: Int, end: Int): String = SU.substring(s, start, end)

    // ------------------------------ Regexp Patterns ------------------------------

    def pat = new Pattern(s)
    def pat(flags: Int) = new Pattern(s, flags)

    def pattern = new PatternWrapper(new Pattern(s))
    def pattern(flags: Int) = new PatternWrapper(new Pattern(s, flags))
    def patternS = new PatternWrapper(new Pattern(s, REFlags.DOTALL))
    def patternI = new PatternWrapper(new Pattern(s, REFlags.IGNORE_CASE))
    def patternIS = new PatternWrapper(new Pattern(s, REFlags.IGNORE_CASE | REFlags.DOTALL))
    def patternIU = new PatternWrapper(new Pattern(s, REFlags.IGNORE_CASE | REFlags.UNICODE))
    def patternISU = new PatternWrapper(new Pattern(s, REFlags.IGNORE_CASE | REFlags.DOTALL | REFlags.UNICODE))

    def matcher(pattern: Pattern): Matcher = pattern.matcher(s)
    def matcher(pw: PatternWrapper): Matcher = pw.matcher(s)
    def matches(pattern: Pattern): Boolean = pattern.matches(s)
    def matches(pw: PatternWrapper): Boolean = pw.matches(s)
    def mapMatch(pw: PatternWrapper): Option[Matcher] = pw.mapMatch(s)
    def mapStartsWith(pw: PatternWrapper): Option[Matcher] = pw.mapStartsWith(s)
    def mapFind(pw: PatternWrapper): Option[Matcher] = pw.mapFind(s)

    // ------------------------------ Other ------------------------------

    /** @see [[java.text.SimpleDateFormat]]*/
    def dateFmt: StdDateFmt = new StdDateFmt(s)
    /** @see [[UrlEncoder.encode]]*/
    def urlEncode: String = UrlEncoder.encode(s)
    /** @see [[UrlEncoder.encodeCyrillic]]*/
    def urlEncodeCyrillic: String = UrlEncoder.encodeCyrillic(s)

    def escapeXml: String = StringEscapeUtils.escapeXml10(s)

    def writeToFile(file: File) {
      val out = new PrintWriter(file, "UTF-8")
      try out.print(s)
      finally out.close()
    }

    @inline def ifNull(onNull: => String): String = if (s == null) onNull else s
  }


  //
  // =================================== Char Wrapper ===================================
  //
  class CharWrapper(c: Char) {
    /**
      * <p>Returns padding using the specified delimiter repeated
      * to a given length.</p>
      *
      * <pre>
      * StringUtils.repeat(0, 'e')  = ""
      * StringUtils.repeat(3, 'e')  = "eee"
      * StringUtils.repeat(-2, 'e') = ""
      * </pre>
      *
      * <p>Note: this method doesn't not support padding with
      * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
      * as they require a pair of {@code char}s to be represented.
      * If you are needing to support full I18N of your applications
      * consider using {@link #repeat(String, int)} instead.
      * </p>
      *
      * c: character to repeat
      * @param repeat number of times to repeat char, negative treated as zero
      * @return String with repeated character
      * @see #repeat(String, int)
      */
    def repeat(repeat: Int): String = SU.repeat(c, repeat)
  }

}
