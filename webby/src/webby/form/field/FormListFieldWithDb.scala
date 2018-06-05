package webby.form.field
import querio._
import webby.commons.text.Plural
import webby.form.{ChangedItemType, Form, FormWithDb, SubForm}


/**
  * @param form
  * @param id
  * @param factory
  * @param recordPlural
  * @tparam PPK Parent form primary key
  * @tparam F   Subform
  * @tparam PK  Subform primary key
  * @tparam TR  Subform TableRecord
  * @tparam MTR Subform MutableTableRecord
  */
abstract class FormListFieldWithDb[PPK, F <: FormWithDb[PK, TR, MTR] with SubForm, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]]
(form: Form,
 id: String,
 factory: () => F,
 recordPlural: Plural,
 keyOps: FormListKeyOps[F#Key])
  extends FormListField[F](form, id, factory, recordPlural, keyOps) {

  // Сохранённые записи всех подформ.
  var savedRecords: Vector[MTR] = Vector.empty

  override def canAddDbConnector: Boolean = false
  override protected def checkAndSetNewKey(v: F) {}

  def load(parentId: PPK)(implicit conn: Conn)
  def save(parentId: PPK, changedItem: Option[(ChangedItemType, Any)])(implicit dt: DataTr)
}


/**
  * Расширенная версия FormListField, поддерживающая работу с БД для подформ, которые связаны
  * с родительской формой полем-ссылкой.
  *
  * @param id          Id поля
  * @param fact        Фабрика создания новой формы
  * @param parentField Поле в таблице table, ссылающееся на родительскую запись.
  * @tparam PPK Parent form primary key
  * @tparam F   Subform
  * @tparam PK  Subform primary key
  * @tparam TR  Subform TableRecord
  * @tparam MTR Subform MutableTableRecord
  */
class FormListFieldWithDbLinked[PPK, F <: FormWithDb[PK, TR, MTR] with SubForm, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR], FIELD <: Table[PK, TR, MTR]#Field[PPK, _]]
(form: Form,
 id: String,
 fact: () => F,
 val parentField: FIELD,
 setter: (FIELD, MTR, PPK) => Any,
 recordPlural: Plural,
 keyOps: FormListKeyOps[F#Key])
  extends FormListFieldWithDb[PPK, F, PK, TR, MTR](form, id, fact, recordPlural, keyOps) {

  override def load(parentId: PPK)(implicit conn: Conn) {
    val builder = Vector.newBuilder[F]
    for (record <- form.base.db.sql(_ selectFrom parentField.table where parentField == parentId fetch())) {
      val subForm = factory()
      subForm.checkDbConnectors()
      subForm.load(record)
      builder += subForm
    }
    setValue(builder.result())
  }

  override def save(parentId: PPK, changedItem: Option[(ChangedItemType, Any)])(implicit dt: DataTr) {
    savedRecords =
      for (form <- get) yield {
        //form.beforeSaveRecord += { r => parentField.set(r, parentId)}
        form.beforeSaveRecord += {r => setter(parentField, r, parentId)}
        form.innerSave(changedItem)
      }
    for (removeId <- removeOldForms.keys) form.base.db.delete(parentField.table, removeId)
  }
}


/**
  * Расширенная версия FormListField, поддерживающая работу с БД для подформ. Эти подформы не связаны
  * с родительской формой.
  *
  * @param id   Id поля
  * @param fact Фабрика создания новой формы
  * @tparam PPK Parent form primary key
  * @tparam F   Subform
  * @tparam PK  Subform primary key
  * @tparam TR  Subform TableRecord
  * @tparam MTR Subform MutableTableRecord
  */
class FormListFieldWithDbStandalone[PPK, F <: FormWithDb[PK, TR, MTR] with SubForm, PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]]
(form: Form,
 id: String,
 fact: () => F,
 recordPlural: Plural,
 keyOps: FormListKeyOps[F#Key])
  extends FormListFieldWithDb[PPK, F, PK, TR, MTR](form, id, fact, recordPlural, keyOps) {

  override def load(parentId: PPK)(implicit conn: Conn): Unit = setNull

  def loadById(id: PK)(implicit conn: Conn): Unit = {
    val form = factory()
    form.base.db.findById(form.table, id) match {
      case Some(record) =>
        form.checkDbConnectors()
        form.load(record)
        setValue(Vector(form))
      case None => setNull
    }
  }

  override def save(parentId: PPK, changedItem: Option[(ChangedItemType, Any)])(implicit dt: DataTr): Unit = {
    savedRecords = for (form <- get) yield form.innerSave(changedItem)
  }
}
