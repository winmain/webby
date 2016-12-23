package lib.form.field
import com.fasterxml.jackson.databind.JsonNode
import lib.html._
import lib.html.elements.SelectHtml
import lib.html.helpers.PageTrait
import webby.commons.text.html._

abstract class BaseSelectField[T](id: String)
  extends ValueField[T] {self =>
  var items: Iterable[T]
  var valueFn: T => String
  var titleFn: T => String
  var emptyTitle: Option[String]

  // ------------------------------- Reading data & js properties -------------------------------
  class SelectJsProps extends BaseJsProps {
    val values: Iterable[String] = for (value <- self.items) yield valueFn(value)
  }
  override def jsProps = new SelectJsProps
  override def parseJsValue(node: JsonNode): Either[String, T] = parseJsString(node) {v =>
    for (item <- items) if (valueFn(item) == v) return Right(item)
    Left("Некорректное значение")
  }
  override def nullValue: T = null.asInstanceOf[T]
  override def toJsValue(v: T): AnyRef = if (isEmpty(v)) null else valueFn(v)

  def defaultHtmlElement()(implicit view: HtmlView): HtmlBase
}


/**
  * Группа выбирашек одного элемента из линейки.
  * Может быть представлена в виде:
  * * горизонтальной ленты
  * * обычного списка радио-баттонов
  * * мобильных табов навигации
  */
class RadioGroupField[T](val id: String,
                         override var items: Iterable[T],
                         override var valueFn: T => String,
                         override var titleFn: T => String,
                         override var emptyTitle: Option[String] = None)
  extends BaseSelectField[T](id) {selfField =>
  override def jsField: String = "radioGroup"

  // ------------------------------- Html helpers -------------------------------

  private def render(topElemCls: String, labelCls: String)(implicit view: HtmlView): HtmlBase = {
    view.div.id(id).cls(topElemCls) {
      val firstIdx = if (emptyTitle.isDefined) -1 else 0
      val lastIdx = items.size - 1
      def renderItem(idx: Int, elId: String, value: String, title: String): Unit = {
        view.inputRadio.id(elId).name(name).valueSafe(value)
        view.label.forId(elId).cls(labelCls).clsIf(idx == firstIdx, "first").clsIf(idx == lastIdx, "last") ~ title
      }
      emptyTitle.foreach(title => renderItem(-1, id + "-", "", title))
      for ((item, idx) <- items.zipWithIndex) {
        val v = valueFn(item)
        val elId = id + "-" + v
        renderItem(idx, elId, v, titleFn(item))
      }
    }
  }

  class Renderer(topElemCls: String, labelCls: String)(implicit view: HtmlView) {
    def main(topTag: CommonTag = view.div)(body: InnerRenderer => Any): HtmlBase =
      topTag.id(id).cls(topElemCls) < body(new InnerRenderer(labelCls))
  }

  class InnerRenderer(labelCls: String)(implicit view: HtmlView) {
    case class Item(item: T) {
      val value: String = valueFn(item)
      val elId: String = id + "-" + value
      def inputRadio: StdInputCheckedTag = view.inputRadio.id(elId).name(name).valueSafe(value)
      def label: StdLabelTag = view.label.forId(elId).cls(labelCls)
    }
    def field: RadioGroupField[T] = selfField
    def withItem(fieldItem: T)(body: Item => Any): Unit = body(Item(fieldItem))
    def items: Iterable[Item] = selfField.items.map(Item.apply)
  }

  /**
    * Стандартная полоска радио-переключателей.
    * Пример: [ A | B | C ]
    */
  def radioStripe()(implicit view: HtmlView): HtmlBase = render("radio-group-stripe", "radio-left")

  /**
    * Список классических радио-переключателей.
    * Пример:
    * () A
    * () B
    * () C
    */
  def radioList()(implicit view: HtmlView): HtmlBase = render("radio-group-list", "radio-left")

  /**
    * Рисование своего элемента. Это может быть полоска типа [[radioStripe()]], список [[radioList()]],
    * или вообще что-то другое.
    *
    * @param topElemCls Класс внешнего элемента, задаёт общий стиль. Например: "radio-group-list", "radio-group-stripe"
    * @param labelCls Класс каждого внутреннего label. Обычно это "radio-label"
    */
  def customRenderer(topElemCls: String, labelCls: String = "radio-left")
                    (implicit view: HtmlView): Renderer =
    new Renderer(topElemCls, labelCls)

  /**
    * Рисование своего списка радио-переключателей, похожего на [[radioList()]]
    */
  def customRadioList(labelCls: String = "radio-left")(implicit view: HtmlView): Renderer = customRenderer("radio-group-list", labelCls)

  /**
    * На десктопной версии используется [[radioStripe]]
    * На мобильной - [[radioList]]
    *
    */
  def radioStripeOrList()(implicit view: HtmlView, page: PageTrait): HtmlBase =
    if (page.desktop) radioStripe()
    else radioList()

  override def defaultHtmlElement()(implicit view: HtmlView): HtmlBase = radioStripe()
}


/**
  * Выпадающий список элементов.
  */
class SelectField[T](val id: String,
                     override var items: Iterable[T],
                     override var valueFn: T => String,
                     override var titleFn: T => String,
                     override var emptyTitle: Option[String] = None)
  extends BaseSelectField[T](id) with PlaceholderField[T] {self =>
  override def jsField: String = "select"

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends SelectJsProps {
    val placeholder: String = self.placeholder
  }
  override def jsProps = new JsProps

  // ------------------------------- Html helpers -------------------------------

  def select(outerSpan: CommonTag => CommonTag = a => a)(implicit view: HtmlView): HtmlBase = {
    SelectHtml()
      .outerSpan(span => outerSpan(span.id(id)))
      .innerSelect(_.name(name))
      .render {
        emptyTitle.foreach(title => view.option.value("") ~ title)
        for (item <- items) {
          val v = valueFn(item)
          view.option.valueSafe(v) ~ titleFn(item)
        }
      }
  }
  override def defaultHtmlElement()(implicit view: HtmlView): HtmlBase = select()
}


/**
  * Довольно специфичное поле radioGroup, у которого нет изначально доступного списка значений.
  * Эти значения генерируются динамически, возможно уже после отрисовки формы.
  * Поэтому, полученное от клиента значение здесь не проверяется.
  * Основное отличие этого поля от какого-нибудь обычного TextField в том, что его параметр jsField = "radioGroup".
  */
class EmptyStringRadioGroupField(val id: String) extends ValueField[String] {
  override def jsField: String = "radioGroup"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null
}
