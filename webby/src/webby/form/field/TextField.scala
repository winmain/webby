package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.commons.text.StdStrHtml
import webby.html.{StdHtmlView, StdInputTag, StdTextareaTag}
import webby.form.{Invalid, Valid, ValidationResult}

/**
 * Текстовое поле
 */
class TextField(val id: String) extends ValueField[String] with PlaceholderField[String] {self =>
  var minLength: Option[Int] = None
  var maxLength: Option[Int] = Some(255)
  var trimSpaces: Boolean = true
  var capitalized: Boolean = false
  var raw: Boolean = false

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val minLength = self.minLength
    val maxLength = self.maxLength
  }
  override def jsProps = new JsProps
  override def jsField: String = "text"
  override def parseJsValue(node: JsonNode): Either[String, String] = {
    var v = node.asText()
    if (v == null) Right(null)
    else {
      if (trimSpaces) v = v.trim
      if (v.isEmpty) Right(null)
      else Right(if (raw) v else StdStrHtml.unescapeAndCleanHtmlEntities(v))
    }
  }

  override def nullValue: String = null

  /** Конвертирует внешнее значение во внутренне значение поля. Вызывается в setValue, silentlySetValue. */
  override protected def convertValue(value: String): String = {
    if (value == null) nullValue
    else {
      val v = if (trimSpaces) value.trim else value
      if (v.isEmpty) nullValue
      else if (capitalized) v.capitalize else v
    }
  }

  // ------------------------------- Builder & validations -------------------------------

  def minLength(v: Int): this.type = { minLength = Some(v); this }
  def maxLength(v: Int): this.type = { maxLength = Some(v); this }
  def trimSpaces(v: Boolean): this.type = { trimSpaces = v; this }
  def capitalize(v: Boolean): this.type = { capitalized = v; this }
  def capitalize: this.type = { capitalized = true; this }
  def rawText: this.type = { raw = true; this }

  /**
   * Проверки, специфичные для конкретной реализации Field.
   * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
   */
  override def validateFieldOnly: ValidationResult = {
    if (minLength.exists(get.length < _)) Invalid("Не менее " + minLength.get + " символов")
    else if (maxLength.exists(get.length > _)) Invalid("Не более " + maxLength.get + " символов")
    else Valid
  }

  // ------------------------------- Html helpers -------------------------------

  def inputText(implicit view: StdHtmlView): StdInputTag = placeholderInputText
  def textarea(implicit view: StdHtmlView): StdTextareaTag = placeholderTextarea
}
