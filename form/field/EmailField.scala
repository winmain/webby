package lib.form.field
import com.fasterxml.jackson.databind.JsonNode
import lib.form.{Invalid, Valid, ValidationResult}
import lib.html.HtmlView
import webby.commons.text.html.StdInputTag
import webby.commons.text.validator.EmailValidator

class EmailField(val id: String) extends ValueField[String] with PlaceholderField[String] {self =>

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
    if (get.length > 250) Invalid("Не более 250 символов")
    else {
      if (EmailValidator.isValid(get)) Valid
      else Invalid("Некорректный email")
    }
  }

  // ------------------------------- Html helpers -------------------------------

  def inputText(implicit view: HtmlView): StdInputTag = placeholderInputText
}
