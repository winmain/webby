package webby.form.field
import querio._
import webby.commons.text.Plural
import webby.form.{ChangedItemType, Form, FormWithDb, SubForm}


abstract class FormListFieldWithDb[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
(form: Form, id: String, factory: () => F, recordPlural: Plural) extends FormListField[F](form, id, factory, recordPlural) {

  // Сохранённые записи всех подформ.
  var savedRecords: Vector[MTR] = Vector.empty

  override def canAddDbConnector: Boolean = false
  override protected def checkAndSetNewKey(v: F) {}

  def load(parentId: Int)(implicit conn: Conn)
  def save(parentId: Int, changedItem: Option[(ChangedItemType, Int)])(implicit dt: DataTr)
}


/**
  * Расширенная версия FormListField, поддерживающая работу с БД для подформ, которые связаны
  * с родительской формой полем-ссылкой.
  *
  * @param id          Id поля
  * @param fact        Фабрика создания новой формы
  * @param parentField Поле в таблице table, ссылающееся на родительскую запись.
  * @tparam F   Форма
  * @tparam TR  Запись в таблице БД
  * @tparam MTR Изменяемая запись в таблице БД
  */
class FormListFieldWithDbLinked[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR], FIELD <: Table[TR, MTR]#Field[Int, _]]
(form: Form, id: String, fact: () => F, val parentField: FIELD, setter: (FIELD, MTR, Int) => Any, recordPlural: Plural)
  extends FormListFieldWithDb[F, TR, MTR](form, id, fact, recordPlural) {

  override def load(parentId: Int)(implicit conn: Conn) {
    val builder = Vector.newBuilder[F]
    for (record <- form.base.db.sql(_ selectFrom parentField.table where parentField == parentId fetch())) {
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
  * @tparam F Форма
  */
class FormListFieldWithDbStandalone[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
(form: Form, id: String, fact: () => F, recordPlural: Plural)
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
