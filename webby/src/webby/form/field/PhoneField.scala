package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.form.Form
import webby.html.{StdHtmlView, StdInputTag}

/**
  * Поле ввода телефона
  *
  * Рекомендуется задать placeholder, например такой: "+7 ___ ___-__-__"
  */
class PhoneField(val form: Form, val id: String) extends ValueField[String] with PlaceholderField[String] {self =>
  // ------------------------------- Reading data & js properties -------------------------------
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node) {v =>
    if (v.indexOf('_') != -1) Right(nullValue)
    else Right(v)
  }
  override def jsField: String = "phone"
  override def nullValue: String = null

  // ------------------------------- Html helpers -------------------------------

  def input(implicit view: StdHtmlView): StdInputTag = placeholderInput(view.inputTel)
}
