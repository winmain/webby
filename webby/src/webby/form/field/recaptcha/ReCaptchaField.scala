package webby.form.field.recaptcha
import com.fasterxml.jackson.databind.JsonNode
import webby.api.mvc.RequestHeader
import webby.form._
import webby.form.field.ValueField

class ReCaptchaField(val form: Form, val reCaptcha: ReCaptcha)(implicit req: RequestHeader) extends ValueField[String] { self =>
  override def shortId: String = reCaptcha.config.formParamName

  required = true

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "reCaptcha"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null

  // ------------------------------- Builder & validations -------------------------------

  private var lastCheckedValue: String = null
  private var lastSolveResult: Boolean = false

  // The captcha check will be executed after all checks and constraints.
  // We cannot execute the captcha check on the same data input twice. So we make sure no errors in
  // the form before the captcha check.
  form.lastConstraints ::= {_ =>
    if (get != lastCheckedValue) {
      lastSolveResult = reCaptcha.solve(get)
      lastCheckedValue = get
    }
    if (!lastSolveResult) error("Попробуйте ещё раз")
    else FormSuccess
  }
}
