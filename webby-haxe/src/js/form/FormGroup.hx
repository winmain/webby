package js.form;

/*
Группа элементов внутри формы, обычно выделяемая фоном (пример: div.cls("form-group")).
Эта панель связана с полями, которые вложены в неё. Также, она имеет элемент вывода ошибки.
 */
class FormGroup {
  public var form(default, null): Form;
  public var tag(default, null): Tag;
  public var error(default, null): FormErrorBlock;

  public function new(form: Form, tag: Tag) {
    this.form = form;
    this.tag = tag;
//    @$el.data('block', @)
    error = new FormErrorBlock(form.config, form.errorBlock, tag, createErrorTags());
    error.resetErrors();
  }

  /*
  Создать и вернуть, или просто вернуть элемент, который будет показывать ошибку этого поля.
   */
  function createErrorTags(): Array<Tag> return [Tag.label.cls(form.config.formGroupErrorClass).cls(form.config.hiddenClass).addTo(tag)];
}
