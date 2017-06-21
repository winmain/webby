package js.form.field;

import js.form.field.Field.FieldProps;
import js.lib.ArrayUtils;

class CheckListField extends Field {
  static public var REG = 'checkList';

  public var checkboxTags(default, null): Array<Tag>;

  public function new(form: Form, props: FieldProps) {
    super(form, props);
    checkboxTags = initCheckboxTags();
  }

  function initCheckboxTags(): Array<Tag> return tag.fndAll('input[type=checkbox]');

  override public function value(): Array<String> {
    var ret: Array<String> = [];
    for (tag in checkboxTags) {
      if (tag.inputElement().checked) ret.push(tag.inputElement().value);
    }
    return ret;
  }

  override public function setValueEl(value: Null<Dynamic>) {
    setValueEl2(value);
  }

  public function setValueEl2(rawValue: Null<Array<String>>) {
    var values: Array<String> = G.or(rawValue, function() return []);
    for (tag in checkboxTags) {
      tag.inputElement().checked = ArrayUtils.contains(values, tag.inputElement().value);
    }
  }
}
