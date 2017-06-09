package js.form.field;

import js.form.field.Field.FieldProps;

class NumberField extends Field {
  static public var REG = 'number';

  public var nullValue: Float;
  public var minValue: Null<Float>;
  public var maxValue: Null<Float>;

  public function new(form: Form, props: NumberFieldProps) {
    super(form, props);
    nullValue = G.or(props.nullValue, function() return 0);
    minValue = props.minValue;
    maxValue = props.maxValue;

    tag.on('blur', function() {
      var v: Dynamic = value();
      if (v != "") {
        if (minValue != null && v < minValue) {
          setJsError(form.config.strings.noLessThanError(minValue));
        }
        if (maxValue != null && v > maxValue) {
          setJsError(form.config.strings.noMoreThanError(maxValue));
        }
      }
    });
  }

  override public function isEmptyValue(v: Dynamic): Bool return v == nullValue;
}


@:build(macros.ExternalFieldsMacro.build())
class NumberFieldProps extends FieldProps {
  public var nullValue: Null<Float>;
  public var minValue: Null<Float>;
  public var maxValue: Null<Float>;
}
