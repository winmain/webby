package js.form.field;

import js.form.field.Field.FieldProps;
import js.html.InputElement;

class RadioGroupField extends Field {
  static public var REG = 'radioGroup';

  public var values: Array<String>;

  public var radios(default, null): Array<Tag>;

  public function new(form: Form, props: RadioGroupFieldProps) {
    super(form, props);
    values = props.values;
    radios = tag.fndAll('input[type=radio]');
  }

  override public function setValueEl(value: Null<Dynamic>) {
    if (value == null) {
      for (radio in radios) radio.inputElement().checked = false;
    } else {
      var radio = findRadio(value);
      if (radio != null) {
        var el: InputElement = cast radio.el;
        el.checked = true;
      } else {
        throw 'Invalid value "$value" for radio group id:$htmlId';
      }
    }
  }

  override public function value(): Dynamic {
    for (radio in radios) {
      var el: InputElement = cast radio.el;
      if (el.checked) {
        return el.value;
      }
    }
    return null;
  }

  @:protected function findRadio(value: Dynamic): Null<Tag> {
    for (radio in radios) {
      var el: InputElement = cast radio.el;
      if (el.value == value) {
        return radio;
      }
    }
    return null;
  }

  public function isValidValue(v: Dynamic): Bool return v == null ? true : G.toBool(findRadio(v));

  override public function focus() {
    onFocus();
  }

  override public function enable(en: Bool) {
    for (radio in radios) radio.attr('disabled', !en);
    box.setCls('disabled', !en);
  }
}

class RadioGroupFieldProps extends FieldProps {
  public var values: Array<String>;
}
