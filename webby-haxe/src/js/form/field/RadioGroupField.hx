package js.form.field;

import js.form.field.Field.FieldProps;

// TODO:
class RadioGroupField extends Field {
  static public var REG = 'radioGroup';

  public function new(form: Form, props: RadioGroupFieldProps) {
    super(form, props);
  }
}

@:build(macros.ExternalFieldsMacro.build())
class RadioGroupFieldProps extends FieldProps {
  public var values: Null<Int>;
}
