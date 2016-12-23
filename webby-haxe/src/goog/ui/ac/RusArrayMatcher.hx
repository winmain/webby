package goog.ui.ac;

import goog.ui.ac.ArrayMatcher;
import haxe.Constraints.Function;
import js.lib.Keyboard;

/*
Расширение ArrayMatcher, дополняющее возможные варианты ввода транслитом.
 */
class RusArrayMatcher<T> extends ArrayMatcher<T> {

  override function requestMatchingRows(token: String, maxMatches: Int, matchHandler: Function, ?opt_fullString: String): Void {
    function getMatches(t: String): Array<T> return
      useSimilar_ ?
      ArrayMatcher.getMatchesForRows(t, maxMatches, rows_) :
      getPrefixMatches(t, maxMatches);

    var matches = getMatches(token);
    if (untyped !matches.length) {
      matches = getMatches(Keyboard.translitFromEnglish(token));
    }
    matchHandler(token, matches);
  }
}
