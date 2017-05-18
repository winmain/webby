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

    tag.on('blur', function() {
      if (!isEmpty()) {
        var v: String = strValue();
        if (minLength > 0 && v.length < minLength) {
          setJsError(form.config.strings.noLessThanCharsError(minLength));
        }
      }
    });
    // TODO: дополнительный код, если будет поддержка jquery.textarea_autosize
    // if @isTextarea = $el[0] && $el[0].tagName == 'TEXTAREA'
    //   $el.textareaAutoSize()
  }

  override function onChange() {
    if (!isEmpty() && maxLength > 0 && strValue().length > maxLength) {
      setJsError(form.config.strings.noMoreThanCharsError(maxLength));
    } else super.onChange();
  }

  function strValue(): String return value();

  // TODO: дополнительный код, если будет поддержка jquery.textarea_autosize
  /*
  onFormShown: ->
    super()
    if @vis && @isTextarea then @$el.textareaAutoSize()
   */
}

@:build(macros.ExternalFieldsMacro.build())
class TextFieldProps extends FieldProps {
  public var minLength: Null<Int>;
  public var maxLength: Null<Int>;
}
