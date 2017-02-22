package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.form.Form
import webby.html.{StdHtmlView, StdInputTag}

/**
  * Скрытое строковое поле
  */
class HiddenField(val form: Form, val id: String) extends ValueField[String] {
  override def jsField: String = "hidden"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null

  // ------------------------------- Html helpers -------------------------------

  def inputHidden(implicit view: StdHtmlView): StdInputTag = view.inputHidden.id(id).name(name)
}

/**
  * Скрытое целочисленное поле
  */
class HiddenIntField(form: Form, id: String) extends BaseIntField(form, id) {
  override def jsField: String = "hidden"

  // ------------------------------- Html helpers -------------------------------

  def inputHidden(implicit view: StdHtmlView): StdInputTag = view.inputHidden.id(id).name(name)
}


/**
  * Скрытое целочисленное поле
  */
class HiddenBooleanField(val form: Form, val id: String) extends ValueField[Boolean] {
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

  def inputHidden(implicit view: StdHtmlView): StdInputTag = view.inputHidden.id(id).name(name)
}
