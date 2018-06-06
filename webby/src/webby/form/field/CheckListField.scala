package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import querio.{MutableTableRecord, Table, TableRecord}
import webby.form.{Form, FormWithDb}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Список чекбоксов
  */
class CheckListField[T](val form: Form,
                        val shortId: String,
                        var items: Iterable[T],
                        var valueFn: T => String,
                        var titleFn: T => String,
                        var commentFn: T => String = null)
  extends ValueField[Iterable[T]] {self =>

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "checkList"
  override def nullValue: Iterable[T] = Nil
  override def parseJsValue(node: JsonNode): Either[String, Iterable[T]] = {
    if (node == null) return Right(nullValue)
    Right(node.asScala.map {nodeEl =>
      val value = nodeEl.asText()
      items.find(valueFn(_) == value).getOrElse(return Left(form.strings.invalidValue))
    })
  }
  override def toJsValue(v: Iterable[T]): AnyRef = v.map(valueFn)

  /** Конвертирует внешнее значение во внутренне значение поля. Вызывается в setValue, silentlySetValue. */
  override protected def convertValue(v: Iterable[T]): Iterable[T] = v.toSet

  override def isEmpty(v: Iterable[T]): Boolean = v.isEmpty

  // ------------------------------- Builder & validations -------------------------------

  def connect[TR <: TableRecord, MTR <: MutableTableRecord[TR]](dbField: Table[TR, MTR]#Field[_, Set[T]])(implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector[TR, MTR](new DbSetFieldConnector[T, TR, MTR](self, dbField))
  def ~:~[TR <: TableRecord, MTR <: MutableTableRecord[TR]](dbField: Table[TR, MTR]#Field[_, Set[T]])(implicit form: FormWithDb[TR, MTR]): this.type = connect(dbField)
}
