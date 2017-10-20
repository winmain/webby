package goog.ui.ac;

import haxe.Constraints.Function;
import js.RegExp;

using js.lib.ArrayUtils;
using js.lib.StrUtils;

/*
Optimized version of goog.ui.ac.ArrayMatcher with `useSimilar` flag turned on.
 */
class SimilarArrayMatcher<T> {

  /*
  Minimum entered char count to show suggestions.
   */
  public var minChars: Int;

  private var rows: Array<T>;

  public var isLetterOrDigit: RegExp = new RegExp('[0-9a-zA-Z]');

  public function new(rows: Array<T>, minChars: Int = 2) {
    this.rows = rows;
    this.minChars = minChars;
  }

  /**
   * Function used to pass matches to the autocomplete
   * @param {string} token Token to match.
   * @param {number} maxMatches Max number of matches to return.
   * @param {Function} matchHandler callback to execute after matching.
   * @param {string=} opt_fullString The full string from the input box.
   */
  public function requestMatchingRows(token: String, maxMatches: Int, matchHandler: Function, ?opt_fullString: String): Void {
    if (G.asBool(minChars) && token.length < minChars) {
      // Check for minChars here
      matchHandler(token, []);
    } else {
      var matches: Array<T> = getSimilarMatchesForRows(token, maxMatches, rows, isLetterOrDigit);
      if (!G.asBool(matches.length)) {
        matches = secondTryMatch(token, maxMatches, rows);
      }

      matchHandler(token, matches);
    }
  }

  public function secondTryMatch(token: String, maxMatches: Int, rows: Array<T>): Array<T> return [];

  // ------------------------------- Static methods -------------------------------

  // Optimized and improved version of goog.ui.ac.ArrayMatcher.getSimilarMatchesForRows
  public static function getSimilarMatchesForRows<T>(token: String, maxMatches: Int, rows: Array<T>, isLetterOrDigit: RegExp): Array<T> {
    var scoredResults: Array<{str: T, score: Float, index: Int}> = [];
    var str: String = token.toLowerCase();
    var maxScore = str.length * 6;
    var arr = str.split('');

    var index = 0;
    while (index < rows.length) {
      var row = rows[index];
      var txt: String = G.toString2(row).toLowerCase();
      var score: Float = 0;

      switch(txt.indexOf(str)) {
        case -1:
          var lastPos = -1;
          var penalty = 10;

          for (c in arr) {
            var pos = txt.indexOf(c);

            if (pos > lastPos) {
              var diff = pos - lastPos - 1;

              if (diff > penalty - 5) {
                diff = penalty - 5;
              }

              score += diff;

              lastPos = pos;
            } else {
              score += penalty;
              penalty += 5;
            }
          }

        case idx:
          if (idx > 0 && !isLetterOrDigit.test(arr[idx - 1])) {
            // Found start of the word
            score = idx / 40;
          } else {
            // Found subtext in the middle of the word
            score = idx / 4;
          }
      }

      if (score < maxScore) {
        scoredResults.push({str: row, score: score, index: index});
      }
      index++;
    }

    scoredResults.sort1(function(a, b) {
      var diff = a.score - b.score;
      if (diff != 0) {
        return cast diff;
      }
      return a.index - b.index;
    });

    var matches: Array<T> = [];
    var i = 0;
    while (i < maxMatches && i < scoredResults.length) {
      matches.push(scoredResults[i].str);
      i++;
    }
    return matches;
  }
}
