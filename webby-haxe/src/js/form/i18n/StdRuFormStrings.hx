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

  // --- UploadField ---

  public function oldBrowserHtml(): String return
    'Вы используете устаревшую версию браузера. ' +
    'Чтобы загружать файлы вам нужно <a href="http://phpbbex.com/oldies/ru.html">сменить браузер</a>.';

  public function errorUploadTitle(): String return 'Ошибка загрузки файла';

  public function unexpectedError(): String return 'Произошла непредвиденная ошибка';
}
