package js.form.field;

import js.form.field.Field.FieldProps;
import js.ui.InputMask;

class RuMonthYearField extends Field {
  static public var REG = 'ruMonthYear';

  public function new(form: Form, props: FieldProps) {
    super(form, props);
    new InputMask().applyOn(tag, "99.9999");
  }
}
