package webby.form.field
import webby.html.{StdHtmlView, StdInputTag, StdTextareaTag}

trait PlaceholderField[T] extends Field[T] {
  var placeholder: String = defaultPlaceholder

  protected def defaultPlaceholder: String = null

  // ------------------------------- Builder & validations -------------------------------

  def placeholder(ph: String): this.type = {placeholder = ph; this}

  // ------------------------------- Html helpers -------------------------------

  protected def placeholderInputText(implicit view: StdHtmlView): StdInputTag = {
    val input = view.inputText.id(id).name(name)
    if (placeholder != null) input.placeholder(placeholder)
    input
  }

  protected def placeholderTextarea(implicit view: StdHtmlView): StdTextareaTag = {
    val textarea: StdTextareaTag = view.textarea.id(id).name(name)
    if (placeholder != null) textarea.placeholder(placeholder)
    textarea
  }

  def placeholderInput[TInput <: StdInputTag](_input: => TInput)(implicit view: StdHtmlView): TInput = {
    val input = _input.id(id).name(name)
    if (placeholder != null) input.placeholder(placeholder)
    input
  }
}
