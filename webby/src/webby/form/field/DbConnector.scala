package webby.form.field
import querio._

trait DbConnector[T, TR <: TableRecord, MTR <: MutableTableRecord[TR]] {
  def field: Field[T]
  def load(r: TR)
  def save(r: MTR)
  def afterSave(r: MTR, original: Option[TR])(implicit dt: DataTr) = {}
}

class StubDbConnector[T, TR <: TableRecord, MTR <: MutableTableRecord[TR]](val field: Field[T]) extends DbConnector[T, TR, MTR] {
  override def load(r: TR): Unit = {}
  override def save(r: MTR): Unit = {}
}

class DbFieldConnector[T, TR <: TableRecord, MTR <: MutableTableRecord[TR]](val field: Field[T], dbField: Table[TR, MTR]#Field[_, T]) extends DbConnector[T, TR, MTR] {
  override def load(r: TR): Unit = field.set(dbField.get(r))
  override def save(r: MTR): Unit = dbField.set(r, field.get)
}

class DbOptionFieldConnector[T, TR <: TableRecord, MTR <: MutableTableRecord[TR]](val field: Field[T], dbField: Table[TR, MTR]#Field[_, Option[T]]) extends DbConnector[T, TR, MTR] {
  override def load(r: TR): Unit = field.set(dbField.get(r))
  override def save(r: MTR): Unit = dbField.set(r, field.getOpt)
}

class DbSetFieldConnector[T, TR <: TableRecord, MTR <: MutableTableRecord[TR]](val field: Field[Iterable[T]], dbField: Table[TR, MTR]#Field[_, Set[T]]) extends DbConnector[Iterable[T], TR, MTR] {
  override def load(r: TR): Unit = field.set(dbField.get(r))
  override def save(r: MTR): Unit = dbField.set(r, field.get.toSet)
}

class DbSetSubTableConnector[T, TR <: TableRecord, MTR <: MutableTableRecord[TR], S, STR <: TableRecord, SMTR <: MutableTableRecord[STR]]
(val field: Field[Iterable[T]],
 getSubTable: TR => SubTableList[STR, SMTR],
 getT: STR => T,
 updater: SubTableUpdater[STR, SMTR, S],
 toS: T => S) extends DbConnector[Iterable[T], TR, MTR] {
  override def load(r: TR): Unit = field := getSubTable(r).items.map(getT)
  override def save(r: MTR): Unit = {}
  override def afterSave(r: MTR, original: Option[TR])(implicit dt: DataTr): Unit =
    updater.update(r._primaryKey, field.get.map(toS)(collection.breakOut), original.map(getSubTable))
}

class DbPasswordFieldConnector[TR <: TableRecord, MTR <: MutableTableRecord[TR]](val field: PasswordField, dbField: Table[TR, MTR]#Field[_, String]) extends DbConnector[String, TR, MTR] {
  override def load(r: TR): Unit = {}
  override def save(r: MTR): Unit = dbField.set(r, field.getHashedPassword)
}
