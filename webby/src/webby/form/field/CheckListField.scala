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
  override def nullValue: Iterable[T] = mutable.Buffer.empty[T]
  override def parseJsValue(node: JsonNode): Either[String, Iterable[T]] = {
    if (node == null) return Right(nullValue)
    Right(node.asScala.map {nodeEl =>
      val value = nodeEl.asText()
      items.find(valueFn(_) == value).getOrElse(return Left(form.strings.invalidValue))
    })
  }

  /** Конвертирует внешнее значение во внутренне значение поля. Вызывается в setValue, silentlySetValue. */
  override protected def convertValue(v: Iterable[T]): Iterable[T] = v.toSet

  override def isEmpty(v: Iterable[T]): Boolean = v.isEmpty

  // ------------------------------- Builder & validations -------------------------------

  def connect[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]](dbField: Table[PK, TR, MTR]#Field[_, Set[T]])(implicit form: FormWithDb[PK, TR, MTR]): this.type =
    dbConnector[PK, TR, MTR](new DbSetFieldConnector[T, PK, TR, MTR](self, dbField))
  def ~:~[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]](dbField: Table[PK, TR, MTR]#Field[_, Set[T]])(implicit form: FormWithDb[PK, TR, MTR]): this.type = connect(dbField)
}
