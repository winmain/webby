package goog.string;

import haxe.extern.Rest;
@:jsRequire('goog.string')
extern class GoogString {
/**
 * Fast prefix-checker.
 * @param {string} str The string to check.
 * @param {string} prefix A string to look for at the start of {@code str}.
 * @return {boolean} True if {@code str} begins with {@code prefix}.
 */
  static function startsWith(str: String, prefix: String): Bool;

/**
 * Fast suffix-checker.
 * @param {string} str The string to check.
 * @param {string} suffix A string to look for at the end of {@code str}.
 * @return {boolean} True if {@code str} ends with {@code suffix}.
 */
  static function endsWith(str: String, suffix: String): Bool;

/**
 * Case-insensitive prefix-checker.
 * @param {string} str The string to check.
 * @param {string} prefix  A string to look for at the end of {@code str}.
 * @return {boolean} True if {@code str} begins with {@code prefix} (ignoring
 *     case).
 */
  static function caseInsensitiveStartsWith(str: String, prefix: String): Bool;

/**
 * Case-insensitive suffix-checker.
 * @param {string} str The string to check.
 * @param {string} suffix A string to look for at the end of {@code str}.
 * @return {boolean} True if {@code str} ends with {@code suffix} (ignoring
 *     case).
 */
  static function caseInsensitiveEndsWith(str: String, suffix: String): Bool;

/**
 * Case-insensitive equality checker.
 * @param {string} str1 First string to check.
 * @param {string} str2 Second string to check.
 * @return {boolean} True if {@code str1} and {@code str2} are the same string,
 *     ignoring case.
 */
  static function caseInsensitiveEquals(str1: String, str2: String): Bool;

/**
 * Does simple python-style string substitution.
 * subs("foo%s hot%s", "bar", "dog") becomes "foobar hotdog".
 * @param {string} str The string containing the pattern.
 * @param {...*} var_args The items to substitute into the pattern.
 * @return {string} A copy of {@code str} in which each occurrence of
 *     {@code %s} has been replaced an argument from {@code var_args}.
 */
  static function subs(str: String, var_args: Rest<Dynamic>): String;

/**
 * Converts multiple whitespace chars (spaces, non-breaking-spaces, new lines
 * and tabs) to a single space, and strips leading and trailing whitespace.
 * @param {string} str Input string.
 * @return {string} A copy of {@code str} with collapsed whitespace.
 */
  static function collapseWhitespace(str: String): String;

/**
 * Checks if a string is empty or contains only whitespaces.
 * @param {string} str The string to check.
 * @return {boolean} Whether {@code str} is empty or whitespace only.
 */
  static function isEmptyOrWhitespace(str: String): Bool;

/**
 * Checks if a string is empty.
 * @param {string} str The string to check.
 * @return {boolean} Whether {@code str} is empty.
 */
  static function isEmptyString(str: String): Bool;

/**
 * Checks if a string is empty or contains only whitespaces.
 *
 * TODO(user): Deprecate this when clients have been switched over to
 * goog.string.isEmptyOrWhitespace.
 *
 * @param {string} str The string to check.
 * @return {boolean} Whether {@code str} is empty or whitespace only.
 */
  static function isEmpty(str: String): Bool;


/**
 * Checks if a string is null, undefined, empty or contains only whitespaces.
 *
 * TODO(user): Deprecate this when clients have been switched over to
 * goog.string.isEmptyOrWhitespaceSafe.
 *
 * @param {*} str The string to check.
 * @return {boolean} Whether {@code str} is null, undefined, empty, or
 *     whitespace only.
 */
  static function isEmptySafe(str: String): Bool;

  // TODO: не все методы перечислены здесь

/**
 * Escapes characters in the string that are not safe to use in a RegExp.
 * @param {*} s The string to escape. If not a string, it will be casted
 *     to one.
 * @return {string} A RegExp safe, escaped copy of {@code s}.
 */
  static function regExpEscape(s: String): String;

/**
 * Returns a string representation of the given object, with
 * null and undefined being returned as the empty string.
 *
 * @param {*} obj The object to convert.
 * @return {string} A string representation of the {@code obj}.
 */
  static function makeSafe(obj: Dynamic): String;
}
