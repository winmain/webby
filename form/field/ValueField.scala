package lib.form.field
import javax.annotation.Nullable

import com.fasterxml.jackson.databind.JsonNode
import lib.form.{FormErrors, FormResult}
import lib.util.text.Str.strWrapper
import webby.api.Logger

import scala.collection.mutable

trait ValueField[T] extends Field[T] {
  def parseJsValue(node: JsonNode): Either[String, T]

  /**
   * Обёртка для чтения строкового js-значения.
   * Здесь идут дополнительные проверки на null, пустую строку. Метод body() получает строку, пропущенную через trim().
   */
  protected def parseJsString(node: JsonNode)(body: String => Either[String, T]): Either[String, T] = {
    var v = node.asText()
    if (v == null) Right(nullValue)
    else {
      v = v.trim
      if (v.isEmpty) Right(nullValue)
      else body(v.unescapeAndCleanHtmlEntities)
    }
  }

  /**
   * Обёртка для чтения целого js-значения.
   * * Здесь идут дополнительные проверки на null, пустую строку.
   */
  protected def parseJsInt(node: JsonNode)(body: Int => Either[String, T]): Either[String, T] = {
    if (node.isNull || node.asText().isEmpty) Right(nullValue)
    else body(node.asInt())
  }

  def setJsValueAndValidate(@Nullable node: JsonNode): FormResult = {
    if (node != null && !node.isNull) {
      try {
        parseJsValue(node) match {
          case Right(v) => set(v); validate
          case Left(error) => FormErrors(errors = mutable.Map(id -> error))
        }
      } catch {
        case e: Exception =>
          Logger(getClass).warn("Error parsing js-value", e)
          FormErrors(errors = mutable.Map(id -> "Некорректное значение поля"))
      }
    } else {
      setNull
      validate
    }
  }
}
