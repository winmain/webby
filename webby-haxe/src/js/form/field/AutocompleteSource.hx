package js.form.field;

import goog.ui.ac.AutoComplete.Matcher;

interface AutocompleteSource {
  function getRowId(row: Dynamic): String;

  function getRowTitle(row: Dynamic): String;

  function getMatcher(fn: String, arg: Dynamic): Matcher;

  function findRowById(matcher: Matcher, id: String): Null<Dynamic>;
}
