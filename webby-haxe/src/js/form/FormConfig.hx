package js.form;

import js.html.XMLHttpRequest;

class FormConfig {
  public function new() {}

  // ------------------------------- Styles -------------------------------

  public var hiddenClass = 'hidden';
  public var hoverClass = 'hover';

  public var formBlockClass = 'form-block';
  public var formErrorsClass = 'form-errors';
  public var blockErrorClass = 'block-error';

  public var fieldBoxClass = 'field';
  public var fieldBoxRequiredClass = 'required';
  public var fieldErrorClass = 'field-error';
  public var fieldBoxErrorClass = 'error';
  public var fieldBoxWithMsgClass = 'with-msg';

  public var showOptionalClass = 'show-optional';
  public var optionalFieldsClass = 'optional-fields';

  public var withErrorClass = 'with-error';

  // ------------------------------- Texts -------------------------------

  public var someFieldsHasErrorText = 'Некоторые поля заполнены неправильно';
  public var onUnloadConfirmText = 'У вас есть несохранённые изменения';

  // ------------------------------- Functions -------------------------------

  public function findSubmitButtons(form: Form): Array<Tag> return form.tag.fndAll('button[type=submit]');

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
