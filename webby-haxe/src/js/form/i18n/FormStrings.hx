package js.form.i18n;

interface FormStrings {

  // ------------------------------- Form -------------------------------

  var someFieldsHasErrorText: String;
  var onUnloadConfirmText: String;

  // ------------------------------- Fields -------------------------------

  function noLessThanError(minValue: Dynamic): String;

  function noMoreThanError(maxValue: Dynamic): String;

  function noLessThanCharsError(minValue: Dynamic): String;

  function noMoreThanCharsError(maxValue: Dynamic): String;

  function notEarlierThanError(min: String): String;

  function noLaterThanError(max: String): String;
}
