package lib.form.field
import com.fasterxml.jackson.databind.JsonNode
import lib.form.{Invalid, Valid, ValidationResult}
import lib.html.HtmlView
import lib.util.text.Plurals
import webby.commons.text.RusPlural
import webby.commons.text.html.{CommonTag, HtmlBase, StdInputTag}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Автокомплит, позволяющий добавить несколько записей.
 * Свободный ввод запрещён, также как и для AutocompleteField.
 */
class AutocompleteListField[T](val id: String,
                               var jsSourceFunction: String,
                               var jsSourceArg: Any = null,
                               var toJs: T => Int,
                               var fromJs: Int => Option[T])
  extends ValueField[Iterable[T]] with PlaceholderField[Iterable[T]] with IterableField[T] {self =>

  var plural: RusPlural = Plurals.recordR
  var minItems: Option[Int] = None
  var maxItems: Option[Int] = None

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val source = self.jsSourceFunction
    val sourceArg = self.jsSourceArg
    val minItems = self.minItems
    val maxItems = self.maxItems
  }
  override def jsProps = new JsProps
  override def jsField: String = "autocompleteList"
  override def nullValue: Iterable[T] = mutable.Buffer.empty[T]
  override def parseJsValue(node: JsonNode): Either[String, Iterable[T]] = {
    if (node == null) return Right(nullValue)
    Right(node.map { nodeEl =>
      val value = nodeEl.asInt()
      fromJs(value).getOrElse(return Left("Некорректное значение"))
    })
  }

  override def toJsValue(v: Iterable[T]): AnyRef = if (v == null) null else v.map(toJs)

  // ------------------------------- Builder & validations -------------------------------

  /** Проверка на минимальное количество элементов (подформ) */
  def minItems(v: Int): this.type = { minItems = Some(v); this }
  /** Проверка на максимальное количество элементов (подформ) */
  def maxItems(v: Int): this.type = { maxItems = Some(v); this }

  /**
   * Проверки, специфичные для конкретной реализации Field.
   * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
   */
  override def validateFieldOnly: ValidationResult = {
    if (minItems.exists(get.size < _)) return Invalid("Не менее " + plural(minItems.get).str)
    if (maxItems.exists(get.size > _)) return Invalid("Не более " + plural(maxItems.get).str)
    Valid
  }

  // ------------------------------- Html helpers -------------------------------

  def inputTextInDiv(itemsTag: CommonTag => CommonTag = a => a, inputTag: StdInputTag => StdInputTag = a => a)(implicit view: HtmlView): HtmlBase = {
    view.div.cls("autocomplete-list") {
      itemsTag(view.div.cls("ac-items clearfix"))
      inputTag(placeholderInputText.attr("autocomplete", "off"))
    }
  }
}
