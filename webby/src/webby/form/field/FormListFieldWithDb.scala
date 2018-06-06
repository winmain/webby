package webby.form.field
import javax.annotation.Nullable
import querio._
import webby.commons.text.Plural
import webby.form.{ChangedItemType, Form, FormWithDb, SubForm}


abstract class FormListFieldWithDb[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
(form: Form,
 id: String,
 factory: () => F,
 recordPlural: Plural)
  extends FormListField[F](form, id, factory, recordPlural) {

  // Сохранённые записи всех подформ.
  var savedRecords: Vector[MTR] = Vector.empty

  override def canAddDbConnector: Boolean = false
  override protected def checkAndSetNewKey(v: F) {}

  def load(parentId: Int)(implicit conn: Conn)
  def save(parentId: Int, changedItem: Option[(ChangedItemType, Int)])(implicit dt: DataTr)
}


/**
  * Extended version FormListField which supports DB for sub-forms.
  * These sub-forms linked with parent form by one field.
  *
  * @param form         Parent form
  * @param id           Field id
  * @param fact         Factory for creating new sub-form
  * @param parentField  Field in sub-form table pointing to parent record.
  * @param setter       `parentField` value setter
  * @param recordPlural Plural form for record
  * @param sortRecords  Records sort function after querying a database.
  *                     If null, sort records by primary key.
  * @tparam F     SubForm
  * @tparam TR    SubForm TableRecord
  * @tparam MTR   SubForm MutableTableRecord
  * @tparam FIELD SubForm Table Field linked to parent id
  */
class FormListFieldWithDbLinked[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR], FIELD <: Table[TR, MTR]#Field[Int, _]]
(form: Form,
 id: String,
 fact: () => F,
 val parentField: FIELD,
 setter: (FIELD, MTR, Int) => Any,
 recordPlural: Plural,
 @Nullable sortRecords: Vector[TR] => Vector[TR] = null)
  extends FormListFieldWithDb[F, TR, MTR](form, id, fact, recordPlural) {

  override def load(parentId: Int)(implicit conn: Conn) {
    val builder = Vector.newBuilder[F]
    val records: Vector[TR] = form.base.db.sql(_ selectFrom parentField.table where parentField == parentId fetch())
    val sortedRecords = if (sortRecords == null) records.sortBy(_._primaryKey) else sortRecords(records)
    for (record <- sortedRecords) {
      val subForm = factory()
      subForm.checkDbConnectors()
      subForm.load(record)
      builder += subForm
    }
    setValue(builder.result())
  }

  override def save(parentId: Int, changedItem: Option[(ChangedItemType, Int)])(implicit dt: DataTr) {
    savedRecords =
      for (form <- get) yield {
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
  * @tparam F Форма
  */
class FormListFieldWithDbStandalone[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
(form: Form,
 id: String,
 fact: () => F,
 recordPlural: Plural)
  extends FormListFieldWithDb[F, TR, MTR](form, id, fact, recordPlural) {

  override def load(parentId: Int)(implicit conn: Conn): Unit = setNull

  def loadById(id: Int)(implicit conn: Conn): Unit = {
    val form = factory()
    form.base.db.findById(form.table, id) match {
      case Some(record) =>
        form.checkDbConnectors()
        form.load(record)
        setValue(Vector(form))
      case None => setNull
    }
  }

  override def save(parentId: Int, changedItem: Option[(ChangedItemType, Int)])(implicit dt: DataTr): Unit = {
    savedRecords = for (form <- get) yield form.innerSave(changedItem)
  }
}
