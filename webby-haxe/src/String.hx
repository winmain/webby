package ;
import haxe.Constraints.Function;
import haxe.extern.EitherType;
import js.RegExp;

/**
	The basic String class.

	A Haxe String is immutable, it is not possible to modify individual
	characters. No method of this class changes the state of `this` String.

	Strings can be constructed using the String literal syntax `"string value"`.

	String can be concatenated by using the `+` operator. If an operand is not a
	String, it is passed through `Std.string()` first.

	@see http://haxe.org/manual/std-String.html
**/
extern class String {

  /**
		The number of characters in `this` String.
	**/
  var length(default, null): Int;

  /**
		Creates a copy from a given String.
	**/
  function new(string: String): Void;

  /**
		Returns a String where all characters of `this` String are upper case.

		Affects the characters `a-z`. Other characters remain unchanged.
	**/
  function toUpperCase(): String;

  /**
		Returns a String where all characters of `this` String are lower case.

		Affects the characters `A-Z`. Other characters remain unchanged.
	**/
  function toLowerCase(): String;

  /**
		Returns the character at position `index` of `this` String.

		If `index` is negative or exceeds `this.length`, the empty String `""`
		is returned.
	**/
  function charAt(index: Int): String;

  /**
		Returns the character code at position `index` of `this` String.

		If `index` is negative or exceeds `this.length`, `null` is returned.

		To obtain the character code of a single character, `"x".code` can be
		used instead to inline the character code at compile time. Note that
		this only works on String literals of length 1.
	**/
  function charCodeAt( index: Int): Null<Int>;

  /**
		Returns the position of the leftmost occurence of `str` within `this`
		String.

		If `startIndex` is given, the search is performed within the substring
		of `this` String starting from `startIndex`. Otherwise the search is
		performed within `this` String. In either case, the returned position
		is relative to the beginning of `this` String.

		If `str` cannot be found, -1 is returned.
	**/
  function indexOf( str: String, ?startIndex: Int ): Int;

  /**
		Returns the position of the rightmost occurence of `str` within `this`
		String.

		If `startIndex` is given, the search is performed within the substring
		of `this` String from 0 to `startIndex`. Otherwise the search is
		performed within `this` String. In either case, the returned position
		is relative to the beginning of `this` String.

		If `str` cannot be found, -1 is returned.
	**/
  function lastIndexOf( str: String, ?startIndex: Int ): Int;

  /**
		Splits `this` String at each occurence of `delimiter`.

		If `this` String is the empty String `""`, the result is not consistent
		across targets and may either be `[]` (on Js, Cpp) or `[""]`.

		If `delimiter` is the empty String `""`, `this` String is split into an
		Array of `this.length` elements, where the elements correspond to the
		characters of `this` String.

		If `delimiter` is not found within `this` String, the result is an Array
		with one element, which equals `this` String.

		If `delimiter` is null, the result is unspecified.

		Otherwise, `this` String is split into parts at each occurence of
		`delimiter`. If `this` String starts (or ends) with `delimiter`, the
		result `Array` contains a leading (or trailing) empty String `""` element.
		Two subsequent delimiters also result in an empty String `""` element.
	**/
  function split(delimiter: EitherType<String, RegExp>, ?limit: Int): Array<String>;

  /**
		Returns `len` characters of `this` String, starting at position `pos`.

		If `len` is omitted, all characters from position `pos` to the end of
		`this` String are included.

		If `pos` is negative, its value is calculated from the end of `this`
		String by `this.length + pos`. If this yields a negative value, 0 is
		used instead.

		If the calculated position + `len` exceeds `this.length`, the characters
		from that position to the end of `this` String are returned.

		If `len` is negative, the result is unspecified.
	**/
  function substr( pos: Int, ?len: Int ): String;

  /**
		Returns the part of `this` String from `startIndex` to but not including `endIndex`.

		If `startIndex` or `endIndex` are negative, 0 is used instead.

		If `startIndex` exceeds `endIndex`, they are swapped.

		If the (possibly swapped) `endIndex` is omitted or exceeds
		`this.length`, `this.length` is used instead.

		If the (possibly swapped) `startIndex` exceeds `this.length`, the empty
		String `""` is returned.
	**/
  function substring( startIndex: Int, ?endIndex: Int ): String;

  /**
		Returns the String itself.
	**/
  function toString(): String;

  /**
		Returns the String corresponding to the character code `code`.

		If `code` is negative or has another invalid value, the result is
		unspecified.
	**/
  @:pure static function fromCharCode(code: Int): String;

  /**
  The slice() method extracts a section of a string and returns a new string.

  `beginSlice` The zero-based index at which to begin extraction. If negative, it is treated as sourceLength + beginSlice where sourceLength is the length of the string (for example, if beginSlice is -3 it is treated as sourceLength - 3).

  `endSlice` Optional. The zero-based index at which to end extraction. If omitted, slice() extracts to the end of the string. If negative, it is treated as sourceLength + endSlice where sourceLength is the length of the string (for example, if endSlice is -3 it is treated as sourceLength - 3).
  **/
  function slice(beginSlice: Int, ?endSlice: Int): String;

  /**
  The match() method retrieves the matches when matching a string against a regular expression.

  `regexp` A regular expression object. If a non-RegExp object obj is passed, it is implicitly converted to a RegExp by using new RegExp(obj).

  Return value
  An Array containing the entire match result and any parentheses-captured matched results; null if there were no matches.
  **/
  function match(regexp: EitherType<RegExp, String>): Array<String>;

  /**
  The localeCompare() method returns a number indicating whether a reference string comes before or after or is the same as the given string in sort order.

  The new locales and options arguments let applications specify the language whose sort order should be used and customize the behavior of the function. In older implementations, which ignore the locales and options arguments, the locale and sort order used are entirely implementation dependent.

  Return value
  A negative number if the reference string occurs before the compare string; positive if the reference string occurs after the compare string; 0 if they are equivalent.
  **/
  function localeCompare(compareString: String, ?locales: String, ?options: Dynamic): Int;

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
  function replace(regexp_or_substr: EitherType<RegExp, String>, newSubStr_or_fn: EitherType<String, Function>): String;
}
