package js.form;

/*
Блок внутри формы (пример: section.cls("form-block")).
Этот блок связан с полями, которые вложены в него. Также, он имеет элемент вывода ошибки.
 */
class FormBlock {
  public var form(default, null): Form;
  public var tag(default, null): Tag;
  public var error(default, null): FormErrorBlock;

  public function new(form: Form, tag: Tag) {
    this.form = form;
    this.tag = tag;
//    @$el.data('block', @)
    error = new FormErrorBlock(form.config, form.errorBlock, tag, createErrorTag());
    error.resetErrors();
  }

  /*
  Создать и вернуть, или просто вернуть элемент, который будет показывать ошибку этого поля.
   */
  function createErrorTag(): Tag return Tag.label.cls(form.config.formBlockErrorClass).cls(form.config.hiddenClass).addTo(tag);
}
