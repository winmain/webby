package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.form.{Form, Invalid, Valid, ValidationResult}

class FloatField(val form: Form, val shortId: String) extends ValueField[Float] with PlaceholderField[Float] {self =>
  var nullValue: Float = Float.NaN
  var minValue: Option[Float] = None
  var maxValue: Option[Float] = None

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val nullValue = float(self.nullValue, Float.NaN)
    val minValue = self.minValue
    val maxValue = self.maxValue
  }
  override def jsProps: BaseJsProps = new JsProps
  override def jsField: String = "number"
  override def parseJsValue(node: JsonNode): Either[String, Float] = {
    if (node.isTextual) {
      val text = node.asText()
      if (text.isEmpty) Right(nullValue)
      else
        try Right(text.toFloat)
        catch {case _: NumberFormatException => Left(form.strings.enterRealNumber)}
    } else
      Right(node.asDouble(nullValue).toFloat)
  }

  // ------------------------------- Builder & validations -------------------------------

  def nullValue(v: Float): this.type = {nullValue = v; this}
  def minValue(v: Float): this.type = {minValue = Some(v); this}
  def maxValue(v: Float): this.type = {maxValue = Some(v); this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minValue.exists(get < _)) Invalid(form.strings.noLessThanError(minValue.get))
    else if (maxValue.exists(get > _)) Invalid(form.strings.noMoreThanError(maxValue.get))
    else Valid
  }
}
