package js.form;

class FormStrings {
  public function new() {}

  // ------------------------------- Form -------------------------------

  public var someFieldsHasErrorText = 'Некоторые поля заполнены неправильно';
  public var onUnloadConfirmText = 'У вас есть несохранённые изменения';

  // ------------------------------- Fields -------------------------------

  public function noLessThanError(minValue: Dynamic) return 'Не менее ' + minValue;
  public function noMoreThanError(maxValue: Dynamic) return 'Не более ' + maxValue;
}
