package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.commons.text.validator.EmailValidator
import webby.form.{Form, Invalid, Valid, ValidationResult}

class EmailField(val form: Form, val shortId: String) extends ValueField[String] with PlaceholderField[String] {self =>

  val MaxLength = 250

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "text"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null

  // ------------------------------- Builder & validations -------------------------------
  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (get.length > MaxLength) Invalid(form.strings.noMoreThanCharsError(MaxLength))
    else {
      if (EmailValidator.isValid(get)) Valid
      else Invalid(form.strings.invalidEmail)
    }
  }
}
