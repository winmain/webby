package js.form.field;

import js.form.field.Field;
import js.ui.InputMask;

class MaskedField extends Field {
  static public var REG = 'masked';

  public var mask: String;

  public function new(form: Form, props: MaskedFieldProps) {
    super(form, props);
    mask = props.mask;
  }

  override public function setValue(v: Null<Dynamic>) {
    super.setValue(v);
    setMask();
  }

  public function setMask(): Void {
    new InputMask().applyOn(tag, mask);
  }
}

class MaskedFieldProps extends FieldProps {
  public var mask: String;
}
