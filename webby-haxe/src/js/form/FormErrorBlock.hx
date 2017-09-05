package js.form;

import goog.array.GoogArray;
import haxe.extern.EitherType;
import js.form.field.Field;
import js.form.field.FormListField;

using js.lib.ArrayUtils;

/*
Блок с ошибками как для группы формы FormConfig.formBlockClass, так и для самой формы FormConfig.formErrorsClass
 */
class FormErrorBlock {
  private var config: FormConfig;
  private var parent: Null<FormErrorBlock>;
  private var parentTag: Tag;
  private var errorTags: Array<Tag>;

  private var errors: Array<EitherType<Field, FormErrorBlock>> = [];
  private var selfErrors: Array<String> = [];
  private var target: Null<Field> = null;

  public function new(config: FormConfig, parent: Null<FormErrorBlock>, parentTag: Tag, errorTags: Array<Tag>) {
    this.config = config;
    this.parent = parent;
    this.parentTag = parentTag;
    this.errorTags = errorTags;
    // Это действие нужно, чтобы срабатывал "фокус" на поля без инпутов (пример: RadioGroupField)

    for (t in errorTags) {
      t.on('mouseover', function() {if (target != null) target.box.cls(config.hoverClass); })
      .on('mouseout', function() {if (target != null) target.box.clsOff(config.hoverClass); })
      .on('click', function() {
        if (target != null) {
          // This error block linked with field, so focus on the field.
          target.box.clsOff(config.hoverClass);
          target.focus();
        } else {
          // No field linked with this error block. This is form error.
          // This error block will not hide automatically, so we hide it here on click.
          resetErrors();
        }
      });
    }
    target = null;
  }

  public function resetErrors() {
    errors = [];
    selfErrors = [];
    for (t in errorTags) t.setHtml('').cls(config.hiddenClass);
    updateErrorTag();
    if (parent != null) parent.clearError(this);
  }

  public function setError(item: EitherType<Field, FormErrorBlock>) {
    if (!GoogArray.contains(errors, item)) {
      errors.push(item);
      updateErrorTag();
      if (parent != null) parent.setError(this);
    }
  }

  public function clearError(item: EitherType<Field, FormErrorBlock>) {
    if (GoogArray.remove(errors, item)) {
      if (errors.isEmpty() && parent != null) {
        parent.clearError(this);
      }
      updateErrorTag();
    }
  }

  public function setSelfErrors(v: Array<String>) {
    selfErrors = v;
    updateErrorTag();
  }

  public function updateErrorTag() {
    var hasErrors: Bool = errors.length > 0;
    if (selfErrors.length > 0) {
      var text: String = selfErrors.join('<br>');
      if (errorTags.nonEmpty()) {
        if (errorTags[0].getHtml() != text) {
          for (t in errorTags) t.setHtml(text).clsOff(config.hiddenClass);
        }
      } else { // В некоторых формах бывает так, что нет блока с ошибками, а саму ошибку показать надо.
        config.showFormErrorDialog(text);
      }
    } else {
      for (t in errorTags) {
        if (hasErrors) t.setHtml(config.strings.someFieldsHasErrorText);
        t.setCls(config.hiddenClass, !hasErrors);
      }
    }
    if (hasErrors) target = getFirstError();
    parentTag.setCls(config.withErrorClass, hasErrors);
    if (parent != null) parent.updateErrorTag();
  }

  public function getFirstError(): Null<Field> {
    var err = errors[0];

    var formErrorBlock: FormErrorBlock = Std.instance(err, FormErrorBlock);
    if (formErrorBlock != null) return formErrorBlock.getFirstError();

    var formListField: FormListField = Std.instance(err, FormListField);
    if (formListField != null) { // Если ошибка на самой подформе, то выбрать первое поле в ней
      var forms = formListField.forms;
      if (forms.length == 0) return formListField;
      else {
        return G.or(forms[0].fields.iterator().next(), function() return formListField);
      }
    }

    return cast err;
  }
}
