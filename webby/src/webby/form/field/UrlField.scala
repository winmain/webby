package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.commons.text.validator.UrlValidator
import webby.form.{Invalid, Valid, ValidationResult}
import webby.html.{StdHtmlView, StdInputTag}

class UrlField(val id: String, allowedDomains: Vector[String] = Vector.empty) extends ValueField[String] with PlaceholderField[String] { self =>
  var allowedSchemes: Array[String] = Array("http", "https")
  var defaultScheme: String = "http"

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "text"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null

  /** Конвертирует внешнее значение во внутренне значение поля. Вызывается в setValue, silentlySetValue. */
  override protected def convertValue(v: String): String = {
    if (v != null && !v.contains("://")) defaultScheme + "://" + v
    else v
  }

  // ------------------------------- Builder & validations -------------------------------
  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (get.length > 250) Invalid("Не более 250 символов")
    else UrlValidator.validate(get, allowedSchemes = allowedSchemes) match {
      case None => Invalid("Некорректная ссылка")
      case Some(url) =>
        if (allowedDomains.nonEmpty && !UrlValidator.validateDomain(url, allowedDomains)) {
          if (allowedDomains.size == 1) return Invalid("Ссылка должна содержать домен " + allowedDomains.head)
          else return Invalid("Ссылка должна содержать один из доменов: " + allowedDomains.mkString(", "))
        }
        Valid
    }
  }

  // ------------------------------- Html helpers -------------------------------

  def inputText(implicit view: StdHtmlView): StdInputTag = placeholderInputText
}
