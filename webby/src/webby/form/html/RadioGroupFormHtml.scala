package webby.form.html
import webby.form.field.RadioGroupField
import webby.html.{CommonTag, HtmlBase, StdInputCheckedTag, StdLabelTag}

trait RadioGroupFormHtml {self: StdFormHtml =>

  // ------------------------------- CSS styles -------------------------------

  protected def radioGroupFirstLabelCls = "first"
  protected def radioGroupLastLabelCls = "last"

  // ------------------------------- Html methods -------------------------------

  protected def radioGroupRender[T](field: RadioGroupField[T], topElemCls: String, labelCls: String): HtmlBase = {
    view.div.id(field.htmlId).cls(form.base.fieldCls).cls(topElemCls) {
      val firstIdx = if (field.emptyTitle.isDefined) -1 else 0
      val lastIdx = field.items.size - 1
      def renderItem(idx: Int, elId: String, value: String, title: String): Unit = {
        view.inputRadio.id(elId).name(field.htmlId).valueSafe(value)
        view.label.forId(elId).cls(labelCls).clsIf(idx == firstIdx, radioGroupFirstLabelCls).clsIf(idx == lastIdx, radioGroupLastLabelCls) ~ title
      }
      field.emptyTitle.foreach(title => renderItem(-1, field.htmlId + "-", "", title))
      for ((item, idx) <- field.items.zipWithIndex) {
        val v = field.valueFn(item)
        val elId = field.htmlId + "-" + v
        renderItem(idx, elId, v, field.titleFn(item))
      }
    }
  }

  class RadioGroupRenderer(field: RadioGroupField[_], topElemCls: String, labelCls: String) {
    def main(topTag: CommonTag = view.div)(body: RadioGroupInnerRenderer[_] => Any): HtmlBase =
      topTag.id(field.htmlId).cls(topElemCls) < body(new RadioGroupInnerRenderer(field, labelCls))
  }

  class RadioGroupInnerRenderer[T](field: RadioGroupField[T], labelCls: String) {
    case class Item(item: T) {
      val value: String = field.valueFn(item)
      val elId: String = field.htmlId + "-" + value
      def inputRadio: StdInputCheckedTag = view.inputRadio.id(elId).name(field.htmlId).valueSafe(value)
      def label: StdLabelTag = view.label.forId(elId).cls(labelCls)
    }
    def withItem(fieldItem: T)(body: Item => Any): Unit = body(Item(fieldItem))
    def items: Iterable[Item] = field.items.map(Item.apply)
  }

  /**
    * Рисование своего элемента. Это может быть полоска типа radioStripe, список radioList(),
    * или вообще что-то другое.
    *
    * @param topElemCls Класс внешнего элемента, задаёт общий стиль. Например: "radio-group-list", "radio-group-stripe"
    * @param labelCls   Класс каждого внутреннего label. Обычно это "radio-label"
    */
  def radioGroupRenderer(field: RadioGroupField[_], topElemCls: String, labelCls: String): RadioGroupRenderer =
    new RadioGroupRenderer(field, topElemCls, labelCls)
}
