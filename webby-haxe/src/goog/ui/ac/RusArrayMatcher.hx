package goog.ui.ac;

import js.RegExp;
import js.lib.Keyboard;

/*
SimilarArrayMatcher with russian transliteration feature.
 */
class RusArrayMatcher<T> extends SimilarArrayMatcher<T> {

  public function new(rows: Array<T>, ?minChars: Int) {
    super(rows, minChars);
    isLetterOrDigit = new RegExp('[0-9a-zA-Zа-яА-Я]');
  }

  override public function secondTryMatch(token: String, maxMatches: Int, rows: Array<T>): Array<T> {
    return SimilarArrayMatcher.getSimilarMatchesForRows(Keyboard.translitFromEnglish(token), maxMatches, rows, isLetterOrDigit);
  }
}
