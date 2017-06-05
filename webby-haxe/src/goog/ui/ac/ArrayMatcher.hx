package goog.ui.ac;

import haxe.Constraints.Function;

/**
 * Basic class for matching words in an array
 * @constructor
 * @param {Array<?>} rows Dictionary of items to match.  Can be objects if they
 *     have a toString method that returns the value to match against.
 * @param {boolean=} opt_noSimilar if true, do not do similarity matches for the
 *     input token against the dictionary.
 */
@:jsRequire('goog.ui.ac.ArrayMatcher')
extern class ArrayMatcher<T> {
  function new(rows: Array<T>, ?opt_noSimilar: Bool);

  @:protected var rows_: Array<T>;

  @:protected var useSimilar_: Bool;

  /**
 * Replaces the rows that this object searches over.
 * @param {Array<?>} rows Dictionary of items to match.
 */
  function setRows(rows: Array<T>): Void;

/**
 * Function used to pass matches to the autocomplete
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @param {Function} matchHandler callback to execute after matching.
 * @param {string=} opt_fullString The full string from the input box.
 */
  function requestMatchingRows(token: String, maxMatches: Int, matchHandler: Function, ?opt_fullString: String): Void;

/**
 * Matches the token against the specified rows, first looking for prefix
 * matches and if that fails, then looking for similar matches.
 *
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @param {!Array<?>} rows Rows to search for matches. Can be objects if they
 *     have a toString method that returns the value to match against.
 * @return {!Array<?>} Rows that match.
 */
  static function getMatchesForRows<T>(token: String, maxMatches: Int, rows: Array<T>): Array<T>;

/**
 * Matches the token against the start of words in the row.
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @return {!Array<?>} Rows that match.
 */
  function getPrefixMatches(token: String, maxMatches: Int): Array<T>;


/**
 * Matches the token against the start of words in the row.
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @param {!Array<?>} rows Rows to search for matches. Can be objects if they
 * have
 *     a toString method that returns the value to match against.
 * @return {!Array<?>} Rows that match.
 */
  static function getPrefixMatchesForRows<T>(token: String, maxMatches: Int, rows: Array<T>): Array<T>;

/**
 * Matches the token against similar rows, by calculating "distance" between the
 * terms.
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @return {!Array<?>} The best maxMatches rows.
 */
  function getSimilarRows(token: String, maxMatches: Int): Array<T>;

/**
 * Matches the token against similar rows, by calculating "distance" between the
 * terms.
 * @param {string} token Token to match.
 * @param {number} maxMatches Max number of matches to return.
 * @param {!Array<?>} rows Rows to search for matches. Can be objects
 *     if they have a toString method that returns the value to
 *     match against.
 * @return {!Array<?>} The best maxMatches rows.
 */
  static function getSimilarMatchesForRows<T>(token: String, maxMatches: Int, rows: Array<T>): Array<T>;
}
