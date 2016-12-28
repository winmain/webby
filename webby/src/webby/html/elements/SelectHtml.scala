package webby.html.elements

import webby.html.{CommonTag, HtmlBase, StdHtmlView, StdSelectTag}

/**
  * Билдер для элемента select по нашим стилям формы.
  *
  * ВАЖНО: Этот элемент нельзя использовать в отрыве от формы, т.к. он требует выполнения SelectField.coffee
  * для своей работы.
  *
  * @see [[webby.form.field.SelectField.select()]] Для примера использования этого класса
  */
case class SelectHtml() {
  private var _outerSpan: CommonTag => CommonTag = a => a
  def outerSpan(fn: CommonTag => CommonTag): this.type = {_outerSpan = fn; this}

  private var _innerSelect: StdSelectTag => StdSelectTag = a => a
  def innerSelect(fn: StdSelectTag => StdSelectTag): this.type = {_innerSelect = fn; this}

  def render(renderOptions: => Any)(implicit view: StdHtmlView): HtmlBase = {
    _outerSpan(view.span.cls("select")) {
      view.label
      _innerSelect(view.select) {
        renderOptions
      }
    }
  }
}
