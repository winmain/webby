package js.form.field;

import js.form.field.Field.FieldProps;

class CheckField extends Field {
  static public var REG = 'check';

  public function new(form: Form, props: FieldProps) {
    super(form, props);
  }

  override public function setValueEl(value: Null<Dynamic>) {
    tagInputEl().checked = value;
  }

  override public function value(): Dynamic return tagInputEl().checked;

  override public function initBoxTag(): Tag return tag.next();
}
