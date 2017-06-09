package webby.html.elements

import webby.form.html.StdFormHtml
import webby.html.{CommonTag, HtmlBase, StdHtmlView, StdSelectTag}

/**
  * Билдер для элемента select по нашим стилям формы.
  *
  * ВАЖНО: Этот элемент нельзя использовать в отрыве от формы, т.к. он требует выполнения SelectField.coffee
  * для своей работы.
  *
  * @see [[StdFormHtml.richSelect()]] Для примера использования этого класса
  */
case class RichSelectHtml(config: RichSelectConfig) {
  private var _outerSpan: CommonTag => CommonTag = a => a
  def outerSpan(fn: CommonTag => CommonTag): this.type = {_outerSpan = fn; this}

  private var _innerSelect: StdSelectTag => StdSelectTag = a => a
  def innerSelect(fn: StdSelectTag => StdSelectTag): this.type = {_innerSelect = fn; this}

  def render(renderOptions: => Any)(implicit view: StdHtmlView): HtmlBase = {
    _outerSpan(view.span.cls(config.outerCls)) {
      view.label.cls(config.labelCls)
      _innerSelect(view.select) {
        renderOptions
      }
    }
  }
}

class RichSelectConfig {
  def outerCls = "richselect"
  def labelCls = "richselect-label"
}
