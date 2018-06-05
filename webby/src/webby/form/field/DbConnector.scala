package webby.form.field
import querio._

import scala.reflect.ClassTag

trait DbConnector[T, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]] {
  def field: Field[T]
  def load(r: TR)
  def save(r: MTR)
  def afterSave(r: MTR, original: Option[TR])(implicit dt: DataTr) = {}
}

trait PreparedDbConnector[T, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]] {
  def forField(formField: Field[T]): DbConnector[T, PK, TR, MTR]
}


class StubDbConnector[T, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]](val field: Field[T]) extends DbConnector[T, PK, TR, MTR] {
  override def load(r: TR): Unit = {}
  override def save(r: MTR): Unit = {}
}

class DbFieldConnector[T, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]](val field: Field[T], dbField: Table[PK, TR, MTR]#Field[_, T]) extends DbConnector[T, PK, TR, MTR] {
  override def load(r: TR): Unit = field.set(dbField.get(r))
  override def save(r: MTR): Unit = dbField.set(r, field.get)
}

class DbOptionFieldConnector[T, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]](val field: Field[T], dbField: Table[PK, TR, MTR]#Field[_, Option[T]]) extends DbConnector[T, PK, TR, MTR] {
  override def load(r: TR): Unit = field.set(dbField.get(r))
  override def save(r: MTR): Unit = dbField.set(r, field.getOpt)
}

class DbSetFieldConnector[T, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]](val field: Field[Iterable[T]], dbField: Table[PK, TR, MTR]#Field[_, Set[T]]) extends DbConnector[Iterable[T], PK, TR, MTR] {
  override def load(r: TR): Unit = field.set(dbField.get(r))
  override def save(r: MTR): Unit = dbField.set(r, field.get.toSet)
}

class DbSetSubTableConnector[T, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR], S, SPK, STR <: TableRecord[SPK], SMTR <: MutableTableRecord[SPK, STR]]
(val field: Field[Iterable[T]],
 getSubTable: TR => SubTableList[SPK, STR, SMTR],
 getT: STR => T,
 updater: SubTableUpdater[PK, SPK, STR, SMTR, S],
 toS: T => S) extends DbConnector[Iterable[T], PK, TR, MTR] {
  override def load(r: TR): Unit = field := getSubTable(r).items.map(getT)
  override def save(r: MTR): Unit = {}
  override def afterSave(r: MTR, original: Option[TR])(implicit dt: DataTr): Unit =
    updater.update(r._primaryKey, field.get.map(toS)(collection.breakOut), original.map(getSubTable))
}

class DbPasswordFieldConnector[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]](val field: PasswordField, dbField: Table[PK, TR, MTR]#Field[_, String]) extends DbConnector[String, PK, TR, MTR] {
  override def load(r: TR): Unit = {}
  override def save(r: MTR): Unit = dbField.set(r, field.getHashedPassword)
}


/**
  * Base class to simplify building [[DbConnector]]s.
  *
  * Example usage:
  * {{{
  *   object OrangeDbConnector extends OneFieldDbConnector[Orange, Int] {
  *     override def formToDb(v: Orange): Option[Int] = ...
  *     override def dbToForm(v: Int): Option[Orange] = ...
  *   }
  *
  *   new FormWithDb[...] {
  *     val orange = someField(...) ~:~ OrangeDbConnector(MyTable.orange)
  *   }
  * }}}
  *
  * @tparam FormFT Form field type
  * @tparam DbFT   Database field type
  */
abstract class OneFieldDbConnector[FormFT, DbFT] {
  def formToDb(v: FormFT): Option[DbFT]
  def dbToForm(v: DbFT): Option[FormFT]

  def apply[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]]
  (dbField: Table[PK, TR, MTR]#Field[_, DbFT]) = new PreparedDbConnector[FormFT, PK, TR, MTR] {
    override def forField(formField: Field[FormFT]): DbConnector[FormFT, PK, TR, MTR] = new DbConnector[FormFT, PK, TR, MTR] {
      override def field: Field[FormFT] = formField
      override def load(r: TR): Unit = formField.set(dbToForm(dbField.get(r)))
      override def save(r: MTR): Unit = dbField.set(r, formToDb(formField.get).getOrElse(sys.error("Field cannot be None")))
    }
  }

  def apply[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]]
  (dbField: Table[PK, TR, MTR]#Field[_, Option[DbFT]])(implicit ct: ClassTag[DbFT]) = new PreparedDbConnector[FormFT, PK, TR, MTR] {
    override def forField(formField: Field[FormFT]): DbConnector[FormFT, PK, TR, MTR] = new DbConnector[FormFT, PK, TR, MTR] {
      override def field: Field[FormFT] = formField
      override def load(r: TR): Unit = formField.set(dbField.get(r).flatMap(dbToForm))
      override def save(r: MTR): Unit = dbField.set(r, formField.getOpt.flatMap(formToDb))
    }
  }
}
