package js.form;

import js.form.field.Field;
import js.html.XMLHttpRequest;

class FormConfig {
  public function new() {}

  public var strings = new FormStrings();

  // ------------------------------- Styles -------------------------------

  public var hiddenClass = 'hidden';
  public var hoverClass = 'hover';

  public var formGroupClass = 'form-group';
  public var formErrorsBlockClass = 'form-errors-block';
  public var formGroupErrorClass = 'form-group-error';

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

  public function findFieldSection(field: Field): Null<Tag> return field.tag.fndParent(function(t: Tag) return t.el.tagName == 'SECTION');

  public function showFormErrorDialog(text: String) {
    G.window.alert(text);
  }

  // Do on resize actions after form shown
  // (rr.windowSize.onResize())
  public function onResizeAfterFormShown() {}

  // rr.util.Actions.onResult(post, [])
  public function onFormSuccess(post: External) {}

  public function onErrorSubmit(xhr: XMLHttpRequest) {
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
}
