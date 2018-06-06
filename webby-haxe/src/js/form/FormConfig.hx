package js.form;

import js.form.field.autocomplete.AutocompleteListConfig;
import js.form.field.autocomplete.AutocompleteSource;
import js.form.field.Field;
import js.form.field.RichSelectConfig;
import js.form.field.upload.UploadConfig;
import js.form.i18n.FormStrings;
import js.html.XMLHttpRequest;

using js.lib.StrUtils;

class FormConfig {
  public function new() {}

  public var strings: FormStrings;

  // ------------------------------- Styles -------------------------------

  public var hiddenClass = 'hidden';
  public var hoverClass = 'hover';

  public var formGroupClass = 'form-group';
  public var formErrorsBlockClass = 'form-errors-block';
  public var formGroupErrorClass = 'form-group-error';
  public var formRowClass = "form-row";

  public var fieldBoxClass = 'field-box';
  public var fieldBoxRequiredClass = 'field-box--required';
  public var fieldBoxErrorClass = 'field-box--error';
  public var fieldBoxWithMsgClass = 'field-box--with-msg';

  public var fieldErrorClass = 'field-error';

  public var showOptionalClass = 'show-optional';
  public var optionalFieldsClass = 'optional-fields';

  public var withErrorClass = 'with-error';

  // ------------------------------- Functions -------------------------------

  public function findSubmitButtons(form: Form): Array<Tag> return form.tag.fndAll('button[type=submit]');

  public function findFieldRow(field: Field): Null<Tag> return field.tag.fndParent(function(t: Tag) return t.hasCls(formRowClass));

  public function showFormErrorDialog(text: String, ?title: String) {
    var s = text.replace(new RegExp('<br>', 'gi'), "\n");
    if (title != null) s = title + "\n\n" + s;
    G.window.alert(s);
  }

  // Do on resize actions after form shown
  // (rr.windowSize.onResize())
  public function onResizeAfterFormShown() {}

  // rr.util.Actions.onResult(post, [])
  public function onFormSuccess(post: External) {}

  public function onErrorSubmit(form: Form, xhr: XMLHttpRequest) {
    G.window.alert('Server error ' + xhr.status);
    /*
    if (xhr.status == 500) {
      rr.window.Message.show({cls: 'error-message', text: 'Внутренняя ошибка сервера. Не волнуйтесь, мы уже работаем над этим.'})
    } else {
      footer = $(rr.window.Message.makeFooter('Повторить', 'btn-orange'))
      footer.find('button').click(-> self.submit())
      rr.window.Message.show(
        cls: 'error-message'
        text: 'Произошла ошибка соединения с сервером.<p class="tech">' + xhr.status + ': ' + err + '</p>'
        footer: footer
      )
    }
    */
  }

  // ------------------------------- Extensions -------------------------------

  public var selectConfig: RichSelectConfig;

  public var autocompleteSource: AutocompleteSource;

  public var uploadConfig: UploadConfig;

  public var acListConfig: AutocompleteListConfig;
}
