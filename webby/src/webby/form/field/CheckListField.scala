package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import querio.{MutableTableRecord, Table, TableRecord}
import webby.form.{Form, FormWithDb}
import webby.html.{CommonTag, HtmlBase, StdHtmlView}

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * Список чекбоксов
  */
class CheckListField[T](val form: Form,
                        val id: String,
                        var items: Iterable[T],
                        var valueFn: T => String,
                        var titleFn: T => String,
                        var commentFn: T => String = null)
  extends ValueField[Iterable[T]] {self =>

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "checkList"
  override def nullValue: Iterable[T] = mutable.Buffer.empty[T]
  override def parseJsValue(node: JsonNode): Either[String, Iterable[T]] = {
    if (node == null) return Right(nullValue)
    Right(node.map {nodeEl =>
      val value = nodeEl.asText()
      items.find(valueFn(_) == value).getOrElse(return Left("Некорректное значение"))
    })
  }

  /** Конвертирует внешнее значение во внутренне значение поля. Вызывается в setValue, silentlySetValue. */
  override protected def convertValue(v: Iterable[T]): Iterable[T] = v.toSet

  override def isEmpty(v: Iterable[T]): Boolean = v.isEmpty

  // ------------------------------- Builder & validations -------------------------------

  def connect[TR <: TableRecord, MTR <: MutableTableRecord[TR]](dbField: Table[TR, MTR]#Field[_, Set[T]])(implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector[TR, MTR](new DbSetFieldConnector[T, TR, MTR](self, dbField))
  def ~:~[TR <: TableRecord, MTR <: MutableTableRecord[TR]](dbField: Table[TR, MTR]#Field[_, Set[T]])(implicit form: FormWithDb[TR, MTR]): this.type = connect(dbField)

  // ------------------------------- Html helpers -------------------------------

  def checkboxList(tag: String = "div", rowWrapper: StdHtmlView => CommonTag = _.div)(implicit view: StdHtmlView): HtmlBase = {
    view.tag(tag).id(id).cls("check-list-field") {
      for {item <- items
           value = valueFn(item)
      } {
        rowWrapper(view) {
          view.inputCheckbox.id(id + "-" + value).valueSafe(value)
          view.label.forId(id + "-" + value).cls("checkbox-left") ~ titleFn(item)
          if (commentFn != null) {
            val comment: String = commentFn(item)
            if (comment != null && !comment.isEmpty) view.div.cls("checkbox-comment") ~ comment
          }
        }
      }
    }
  }
}
