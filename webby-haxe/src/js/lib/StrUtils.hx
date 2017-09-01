package js.lib;

import haxe.Constraints.Function;
import haxe.extern.EitherType;

class StrUtils {
  // ------------------------------- Javascript string -------------------------------

  /**
  The slice() method extracts a section of a string and returns a new string.

  `beginSlice` The zero-based index at which to begin extraction. If negative, it is treated as sourceLength + beginSlice where sourceLength is the length of the string (for example, if beginSlice is -3 it is treated as sourceLength - 3).

  `endSlice` Optional. The zero-based index at which to end extraction. If omitted, slice() extracts to the end of the string. If negative, it is treated as sourceLength + endSlice where sourceLength is the length of the string (for example, if endSlice is -3 it is treated as sourceLength - 3).
  **/
  public static inline function slice(s: String, beginSlice: Int, ?endSlice: Int): String {
    if (endSlice == null) {
      return untyped __js__("{0}.slice({1})", s, beginSlice);
    } else {
      return untyped __js__("{0}.slice({1}, {2})", s, beginSlice, endSlice);
    }
  }

  /**
  The match() method retrieves the matches when matching a string against a regular expression.

  `regexp` A regular expression object. If a non-RegExp object obj is passed, it is implicitly converted to a RegExp by using new RegExp(obj).

  Return value
  An Array containing the entire match result and any parentheses-captured matched results; null if there were no matches.
  **/
  public static inline function match(s: String, regexp: EitherType<RegExp, String>): Array<String> return
    untyped __js__("{0}.match({1})", s, regexp);

  /**
  The localeCompare() method returns a number indicating whether a reference string comes before or after or is the same as the given string in sort order.

  The new locales and options arguments let applications specify the language whose sort order should be used and customize the behavior of the function. In older implementations, which ignore the locales and options arguments, the locale and sort order used are entirely implementation dependent.

  Return value
  A negative number if the reference string occurs before the compare string; positive if the reference string occurs after the compare string; 0 if they are equivalent.
  **/
  public static inline function localeCompare(s: String, compareString: String, ?locales: String, ?options: Dynamic): Int return
    if (locales == null && options == null) {
      return untyped __js__("{0}.localeCompare({1})", s, compareString);
    } else if (options == null) {
      return untyped __js__("{0}.localeCompare({1}, {2})", s, compareString, locales);
    } else {
      return untyped __js__("{0}.localeCompare({1}, {2}, {3})", s, compareString, locales, options);
    }

  /**
  The replace() method returns a new string with some or all matches of a pattern replaced by a replacement. The pattern can be a string or a RegExp, and the replacement can be a string or a function to be called for each match.

  `regexp` (pattern)
    A RegExp object or literal. The match or matches are replaced with newSubStr or the value returned by the specified function.
  `substr` (pattern)
    A String that is to be replaced by newSubStr. It is treated as a verbatim string and is not interpreted as a regular expression. Only the first occurrence will be replaced.
  `newSubStr` (replacement)
    The String that replaces the substring specified by the specified regexp or substr parameter. A number of special replacement patterns are supported; see the "Specifying a string as a parameter" section below.
  `function (replacement)`
    A function to be invoked to create the new substring to be used to replace the matches to the given regexp or substr. The arguments supplied to this function are described in the "Specifying a function as a parameter" section below.
  **/
  public static inline function replace(s: String, substr: EitherType<RegExp, String>, newSubStr: EitherType<String, Function>): String return
    untyped __js__("{0}.replace({1}, {2})", s, substr, newSubStr);

  // ------------------------------- Common string utils -------------------------------

  /*
  Left pad a String with zeroes.
   */
  public static function zpad(num: EitherType<Float, String>, zeroes: Int): String {
    return slice('0' + num, -zeroes);
  }
}
