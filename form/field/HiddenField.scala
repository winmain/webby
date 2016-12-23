package lib.form.field
import com.fasterxml.jackson.databind.JsonNode
import lib.html.HtmlView
import webby.commons.text.html.StdInputTag

/**
  * Скрытое строковое поле
  */
class HiddenField(val id: String) extends ValueField[String] {
  override def jsField: String = "hidden"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null

  // ------------------------------- Html helpers -------------------------------

  def inputHidden(implicit view: HtmlView): StdInputTag = view.inputHidden.id(id).name(name)
}

/**
  * Скрытое целочисленное поле
  */
class HiddenIntField(id: String) extends BaseIntField(id) {
  override def jsField: String = "hidden"

  // ------------------------------- Html helpers -------------------------------

  def inputHidden(implicit view: HtmlView): StdInputTag = view.inputHidden.id(id).name(name)
}


/**
  * Скрытое целочисленное поле
  */
class HiddenBooleanField(val id: String) extends ValueField[Boolean] {
  override def jsField: String = "hidden"
  override def parseJsValue(node: JsonNode): Either[String, Boolean] = parseJsString(node) {v =>
    try Integer.parseInt(v) match {
      case 0 => Right(false)
      case 1 => Right(true)
      case _ => Left("Некорректное значение")
    }
    catch {case e: NumberFormatException => Left("Введите целое число")}
  }
  override def nullValue: Boolean = false

  // ------------------------------- Html helpers -------------------------------

  def inputHidden(implicit view: HtmlView): StdInputTag = view.inputHidden.id(id).name(name)
}
