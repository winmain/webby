package js.form.field;

import js.form.field.Field.FieldProps;

class IntField extends Field {
  static public var REG = 'int';

  public var nullValue: Int;
  public var minValue: Null<Int>;
  public var maxValue: Null<Int>;

  public function new(form: Form, props: IntFieldProps) {
    super(form, props);
    nullValue = G.or(props.nullValue, function() return 0);
    minValue = props.minValue;
    maxValue = props.maxValue;

    // TODO:
  }

  // TODO: public function isEmpty():Bool
}

/*
class rr.form.field.IntField extends rr.form.field.BaseField
  constructor: (form, props) ->
    super(form, props)
    self = @
    @nullValue = props['nullValue'] or 0
    @minValue = props['minValue']
    @maxValue = props['maxValue']

    @$el.blur(->
      value = self.value()
      if value
        if self.minValue != null && value < self.minValue
          self.setJsError('Не менее ' + self.minValue)
        if self.maxValue != null && value > self.maxValue
          self.setJsError('Не более ' + self.maxValue)
    )


  isEmpty: -> @value == @nullValue

 */

@:build(macros.ExternalFieldsMacro.build())
class IntFieldProps extends FieldProps {
  public var nullValue: Null<Int>;
  public var minValue: Null<Int>;
  public var maxValue: Null<Int>;
}
