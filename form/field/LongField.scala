package lib.form.field
import com.fasterxml.jackson.databind.JsonNode
import lib.form.{Invalid, ValidationResult}
import lib.html.HtmlView
import webby.commons.text.html.StdInputTag

/**
  * Базовый класс для длинного целого числа (без minValue, maxValue)
  */
class BaseLongField(val id: String) extends ValueField[Long] with PlaceholderField[Long] { self =>
  var nullValue: Long = 0L

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val nullValue = long(self.nullValue, 0L)
  }
  override def jsProps: BaseJsProps = new JsProps
  override def jsField: String = "int"
  override def parseJsValue(node: JsonNode): Either[String, Long] = {
    if (node.isTextual) {
      val text = node.asText()
      if (text.isEmpty) Right(nullValue)
      else
        try Right(java.lang.Long.parseLong(text))
        catch {case e: NumberFormatException => Left("Введите целое число")}
    } else
      Right(node.asLong(nullValue))
  }

  // ------------------------------- Builder & validations -------------------------------

  def nullValue(v: Long): this.type = {nullValue = v; this}

  // ------------------------------- Html helpers -------------------------------

  def inputNumber(implicit view: HtmlView): StdInputTag = placeholderInputText
}

/**
  * Поле для длинного целого числа
  */
class LongField(id: String) extends BaseLongField(id) { self =>
  var minValue: Option[Long] = None
  var maxValue: Option[Long] = None

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val nullValue = long(self.nullValue, 0L)
    val minValue = self.minValue
    val maxValue = self.maxValue
  }
  override def jsProps: BaseJsProps = new JsProps

  // ------------------------------- Builder & validations -------------------------------

  def minValue(v: Long): this.type = {minValue = Some(v); this}
  def maxValue(v: Long): this.type = {maxValue = Some(v); this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minValue.exists(get < _)) Invalid("Не менее " + minValue.get)
    else if (maxValue.exists(get > _)) Invalid("Не более " + maxValue.get)
    else super.validateFieldOnly
  }
}
