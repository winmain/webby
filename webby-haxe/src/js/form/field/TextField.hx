package js.form.field;

import js.form.field.Field.FieldProps;

// goog.require 'jquery.textarea_autosize' # Только для textarea

class TextField extends Field {
  static public var REG = 'text';

  public var minLength: Null<Int>;
  public var maxLength: Null<Int>;

  public function new(form: Form, props: TextFieldProps) {
    super(form, props);
    minLength = props.minLength;
    maxLength = props.maxLength;

    // TODO:
  }

  // TODO:
}

/*
class rr.form.field.TextField extends rr.form.field.BaseField
  constructor: (form, props) ->
    super(form, props)
    self = @
    @minLength = props['minLength']
    @maxLength = props['maxLength']

    $el = @$el
    $el.blur(->
      value = self.value()
      if value
        if self.minLength && value.length < self.minLength
          self.setJsError('Не менее ' + self.minLength + ' символов')
    )
    if @isTextarea = $el[0] && $el[0].tagName == 'TEXTAREA'
      $el.textareaAutoSize()

  onChange: ->
    val = @value()
    if val && @maxLength && val.length > @maxLength
      @setJsError('Не более ' + @maxLength + ' символов')
    else super()

  onFormShown: ->
    super()
    if @vis && @isTextarea then @$el.textareaAutoSize()

 */

@:build(macros.ExternalFieldsMacro.build())
class TextFieldProps extends FieldProps {
  public var minLength: Null<Int>;
  public var maxLength: Null<Int>;
}
