package webby.form
import querio.{Transaction, _}
import webby.commons.text.Plural

abstract class BaseForms {self =>
  def db: DbTrait

  def longTextFieldMaxLength = 4000

  def recordPlural: Plural
  def recordRPlural: Plural

  def maybeChangedFieldsDao: Option[ChangedFieldsDao] = None

  trait WithDb[TR <: TableRecord, MTR <: MutableTableRecord[TR]] extends FormWithDb[TR, MTR] {
    override type B = self.type
    override def base: B = self
  }
}


trait ChangedItemType


trait ChangedFieldsDao {
  case class ChangedFieldValue(fieldTitle: String, path: String, value: String)

  def query(tpe: ChangedItemType, item: TableRecord): Vector[ChangedFieldValue]

  // ------------------------------- Modification methods -------------------------------

  def insert(tpe: ChangedItemType, itemId: Int, field: AnyTable#ThisField, id: Int)(implicit tr: Transaction)

  def delete(tpe: ChangedItemType, itemId: Int)(implicit tr: Transaction): Unit
}
