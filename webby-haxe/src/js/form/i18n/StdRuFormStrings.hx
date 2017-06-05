package js.form.i18n;

class StdRuFormStrings implements FormStrings {
  public function new() {}

  // ------------------------------- Form -------------------------------

  public var someFieldsHasErrorText = 'Некоторые поля заполнены неправильно';
  public var onUnloadConfirmText = 'У вас есть несохранённые изменения';

  // ------------------------------- Fields -------------------------------

  public function noLessThanError(minValue: Dynamic) return 'Не менее ' + minValue;

  public function noMoreThanError(maxValue: Dynamic) return 'Не более ' + maxValue;

  public function noLessThanCharsError(minValue: Dynamic) return 'Не менее ' + minValue + ' символов';

  public function noMoreThanCharsError(maxValue: Dynamic) return 'Не более ' + maxValue + ' символов';

  public function notEarlierThanError(min: String): String return "Не ранее " + min;

  public function noLaterThanError(max: String): String return "Не позднее " + max;
}
