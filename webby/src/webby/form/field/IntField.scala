package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.commons.collection.Pager
import webby.form.{Form, Invalid, Valid, ValidationResult}

/**
  * Базовый класс для целого числа (без minValue, maxValue)
  */
class BaseIntField(val form: Form, val shortId: String) extends ValueField[Int] with PlaceholderField[Int] {self =>
  var nullValue: Int = 0

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val nullValue = int(self.nullValue, 0)
  }
  override def jsProps: BaseJsProps = new JsProps
  override def jsField: String = "number"
  override def parseJsValue(node: JsonNode): Either[String, Int] = {
    if (node.isTextual) {
      val text = node.asText()
      if (text.isEmpty) Right(nullValue)
      else
        try Right(Integer.parseInt(text))
        catch {case e: NumberFormatException => Left("Введите целое число")}
    } else
      Right(node.asInt(nullValue))
  }

  // ------------------------------- Builder & validations -------------------------------

  def nullValue(v: Int): this.type = {nullValue = v; this}
}

/**
  * Поле для целого числа
  */
class IntField(form: Form, id: String) extends BaseIntField(form, id) {self =>
  var minValue: Option[Int] = None
  var maxValue: Option[Int] = None

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val nullValue = int(self.nullValue, 0)
    val minValue = self.minValue
    val maxValue = self.maxValue
  }
  override def jsProps: BaseJsProps = new JsProps

  // ------------------------------- Builder & validations -------------------------------

  def minValue(v: Int): this.type = {minValue = Some(v); this}
  def maxValue(v: Int): this.type = {maxValue = Some(v); this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minValue.exists(get < _)) Invalid("Не менее " + minValue.get)
    else if (maxValue.exists(get > _)) Invalid("Не более " + maxValue.get)
    else Valid
  }
}

/**
  * Пэйджер (понадобился для CRM)
  */
class PagerField(val form: Form, var step: Int, var nearRadius: Int, val shortId: String = "page") extends ValueField[Int] {self =>
  override def jsField: String = "pager"

  override def parseJsValue(node: JsonNode): Either[String, Int] = Right(node.asInt())
  override def nullValue: Int = 1

  def pager: Pager = Pager(step, get)

  // ------------------------------- Html helpers -------------------------------

  //  def pagerComponent()(implicit view: StdHtmlView): HtmlBase = pagerComponent(get * step + 1)
  //
  //  def pagerComponent(totalCount: Int)(implicit view: StdHtmlView): HtmlBase = {
  //    view.div.id(id + "-pager") {
  //      view.inputHidden.id(id).name(name).value(get)
  //      PagerComponent.linkTags(pager.helper(totalCount, nearRadius),
  //        (a, page) => a.hrefAnchor.attr("page", page))
  //        .writeComponent()(view.buf)
  //    }
  //  }
}
