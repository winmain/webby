package js.form.field;

import js.form.field.Field.FieldProps;

class ReCaptchaField extends Field {
  static public var REG = 'reCaptcha';

  public function new(form: Form, props: FieldProps) {
    super(form, props);
  }

  override function initHtmlId(props: FieldProps): String return props.shortId;

  override public function initTag(): Tag return form.tag.fnd('.g-recaptcha');

  override public function initBoxTag(): Tag return tag;

  override public function value(): Dynamic {
    var tag = form.tag.fnd('#' + shortId);
    return tag != null ? tag.val() : null;
  }
}
