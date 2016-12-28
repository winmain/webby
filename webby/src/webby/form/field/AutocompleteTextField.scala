package webby.form.field
import webby.html.{StdHtmlView, StdInputTag}

/**
 * Текстовое поле, аналогичное TextField, но с поддержкой необязательного автокомплита.
 */
class AutocompleteTextField(id: String,
                            var jsSourceFunction: String,
                            var jsSourceArg: Any = null) extends TextField(id) {self =>

  // ------------------------------- Reading data & js properties -------------------------------
  class AutocompleteJsProps extends JsProps {
    val source = self.jsSourceFunction
    val sourceArg = self.jsSourceArg
  }
  override def jsProps = new AutocompleteJsProps
  override def jsField: String = "autocompleteText"

  // ------------------------------- Html helpers -------------------------------

  override def inputText(implicit view: StdHtmlView): StdInputTag = placeholderInputText.attr("autocomplete", "off")
}
