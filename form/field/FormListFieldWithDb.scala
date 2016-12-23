package lib.form.field
import lib.db.Db
import lib.form.FormWithDb
import models.adm.enums.ChangedItemType
import querio._


abstract class FormListFieldWithDb[F <: FormWithDb[TR, MTR], TR <: TableRecord, MTR <: MutableTableRecord[TR]]
(id: String, factory: () => F) extends FormListField[F](id, factory) {

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
 * @param id Id поля
 * @param factory Фабрика создания новой формы
 * @param parentField Поле в таблице table, ссылающееся на родительскую запись.
 * @tparam F Форма
 * @tparam TR Запись в таблице БД
 * @tparam MTR Изменяемая запись в таблице БД
 */
class FormListFieldWithDbLinked[F <: FormWithDb[TR, MTR], TR <: TableRecord, MTR <: MutableTableRecord[TR], FIELD <: Table[TR, MTR]#Field[Int, _]]
(id: String, factory: () => F, val parentField: FIELD, setter: (FIELD, MTR, Int) => Any)
  extends FormListFieldWithDb[F, TR, MTR](id, factory) {

  override def load(parentId: Int)(implicit conn: Conn) {
    val builder = Vector.newBuilder[F]
    for (record <- Db.sql(_ selectFrom parentField.table where parentField == parentId fetch())) {
      val form = factory()
      form.checkDbConnectors()
      form.load(record)
      builder += form
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
    for (removeId <- removeOldForms.keys) Db.delete(parentField.table, removeId)
  }
}


/**
 * Расширенная версия FormListField, поддерживающая работу с БД для подформ. Эти подформы не связаны
 * с родительской формой.
 *
 * @param id Id поля
 * @param factory Фабрика создания новой формы
 * @tparam F Форма
 */
class FormListFieldWithDbStandalone[F <: FormWithDb[TR, MTR], TR <: TableRecord, MTR <: MutableTableRecord[TR]]
(id: String, factory: () => F)
  extends FormListFieldWithDb[F, TR, MTR](id, factory) {

  override def load(parentId: Int)(implicit conn: Conn): Unit = setNull

  def loadById(id: Int)(implicit conn: Conn): Unit = {
    val form = factory()
    Db.findById(form.table, id) match {
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
