package js.form.field;

import js.form.field.Field.FieldProps;

class HiddenField extends Field {
  static public var REG = 'hidden';

  public function new(form: Form, props: FieldProps) {
    super(form, props);
  }
}
