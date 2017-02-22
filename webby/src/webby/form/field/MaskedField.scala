package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.form.Form
import webby.html.{StdHtmlView, StdInputTag}

/**
  * @author Oleg Arshinsky
  */
class MaskedField(val form: Form, val id: String, val mask: String) extends ValueField[String] with PlaceholderField[String] {self =>

  // ------------------------------- Reading data & js properties -------------------------------
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node) {v =>
    if (v == null || v.isEmpty) Right(nullValue)
    else Right(v)
  }

  override def jsField: String = "masked"
  override def nullValue: String = null


  // ------------------------------- Html helpers -------------------------------

  object JsProps extends BaseJsProps {
    val mask = self.mask
  }

  override def jsProps: BaseJsProps = JsProps

  def inputText(implicit view: StdHtmlView): StdInputTag = placeholderInputText
}

