package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.form.Form

abstract class BaseSelectField[T](val form: Form, id: String)
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
}


/**
  * Группа выбирашек одного элемента из линейки.
  * Может быть представлена в виде:
  * * горизонтальной ленты
  * * обычного списка радио-баттонов
  * * мобильных табов навигации
  */
class RadioGroupField[T](form: Form,
                         val shortId: String,
                         override var items: Iterable[T],
                         override var valueFn: T => String,
                         override var titleFn: T => String,
                         override var emptyTitle: Option[String] = None)
  extends BaseSelectField[T](form, shortId) {selfField =>
  override def jsField: String = "radioGroup"
}


/**
  * Выпадающий список элементов.
  */
class RichSelectField[T](form: Form,
                         val shortId: String,
                         override var items: Iterable[T],
                         override var valueFn: T => String,
                         override var titleFn: T => String,
                         override var emptyTitle: Option[String] = None)
  extends BaseSelectField[T](form, shortId) with PlaceholderField[T] {self =>
  override def jsField: String = "richSelect"

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends SelectJsProps {
    val placeholder: String = self.placeholder
  }
  override def jsProps = new JsProps
}


/**
  * Довольно специфичное поле radioGroup, у которого нет изначально доступного списка значений.
  * Эти значения генерируются динамически, возможно уже после отрисовки формы.
  * Поэтому, полученное от клиента значение здесь не проверяется.
  * Основное отличие этого поля от какого-нибудь обычного TextField в том, что его параметр jsField = "radioGroup".
  */
class EmptyStringRadioGroupField(val form: Form, val shortId: String) extends ValueField[String] {
  override def jsField: String = "radioGroup"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null
}
