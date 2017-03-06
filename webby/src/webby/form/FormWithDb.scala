package webby.form
import querio._
import webby.api.mvc.{ResultException, Results}
import webby.form.field.{DbConnector, Field}

import scala.collection.mutable

trait FormWithDb[TR <: TableRecord, MTR <: MutableTableRecord[TR]] extends Form {
  implicit def thisFormWithDb: FormWithDb[TR, MTR] = this

  protected var _original: Option[TR] = None
  def original: Option[TR] = _original

  protected val dbConnectors: mutable.Buffer[DbConnector[_, TR, MTR]] = mutable.Buffer[DbConnector[_, TR, MTR]]()
  protected[form] def addFieldDbConnector(dbConnector: DbConnector[_, TR, MTR]): Unit = dbConnectors += dbConnector

  /**
    * Действия, проверяющие корректные ли мы данные загрузили?
    * Для основных форм (не подформ) это действие обязательно должно быть задано.
    * Форма будет загружена только если все эти действия вернут true.
    */
  val checkLoadedRecord: mutable.Buffer[TR => Boolean] = mutable.Buffer.empty

  /**
    * Действия, выполняемые после загрузки формы (если она была загружена) в методе load().
    */
  val afterLoadRecord: mutable.Buffer[TR => Any] = mutable.Buffer.empty

  /**
    * Действие, аналогичное afterLoadRecord, но с дополнительным контекстом, содержащим conn (для запросов в БД).
    * Вызывается после afterLoadRecord
    */
  val afterLoadRecord2: mutable.Buffer[AfterLoadRecordCtx => Any] = mutable.Buffer.empty

  /**
    * Действие, которое оборачивает внутри себя весь механизм сохранения, включая вызовы:
    * - beforeSaveRecord
    * - beforeSaveRecord2
    * - afterSaveRecord
    */
  private var wrapSaveRecordFn: WrapSaveRecordCtx => Unit = null
  def wrapSaveRecord(fn: WrapSaveRecordCtx => Unit): Unit = {
    require(wrapSaveRecordFn == null, "Cannot override wrapSaveRecordFn twice")
    wrapSaveRecordFn = fn
  }

  /**
    * Действие, вызывающееся после заполнения MTR, но перед самим сохранением записи в БД.
    * Внимание! Если эта форма является подформой, то beforeSaveRecord перезаписывается в классе FormListFieldWithDb.
    */
  val beforeSaveRecord: mutable.Buffer[MTR => Any] = mutable.Buffer.empty

  /**
    * Действие, аналогичное beforeSaveRecord, но с дополнительным контекстом, содержащим Db.ModifyData, Transaction
    * Вызывается после beforeSaveRecord.
    */
  val beforeSaveRecord2: mutable.Buffer[BeforeSaveRecordCtx => Any] = mutable.Buffer.empty

  /**
    * Вызывается после сохранения формы
    */
  val afterSaveRecord: mutable.Buffer[AfterSaveRecordCtx => Any] = mutable.Buffer.empty

  // ------------------------------- Abstract methods -------------------------------

  def table: Table[TR, MTR]
  def loadRecord(r: TR) {}
  def saveRecord(r: MTR) {}
  /** Если задан этот параметр, то форма при сохранении, будет создавать записи изменённых полей в таблице changed_fields */
  def saveChangedItemType: Option[ChangedItemType] = None
  /** Набор полей, которые не нужно записывать в таблицу changed_fields */
  def ignoreToAddChangedFields: Set[Table[TR, MTR]#Field[_, _]] = Set.empty

  // ------------------------------- Public methods -------------------------------

  def checkDbConnectors() {
    if (dbConnectors.size != fields.size - formFieldsWithDb.size) {
      sys.error("Some fields does not have DbConnector: " +
        fields.withFilter(f => !formFieldsWithDb.contains(f) && !dbConnectors.exists(_.field == f)).map(_.shortId).mkString(", "))
    }
  }

  def load(id: Int)(implicit conn: Conn): Boolean = {
    base.db.sql(_.findById(table, id)) match {
      case Some(record) if checkLoadedRecord.forall(_ (record)) => load(record); true
      case _ => false
    }
  }

  def load(record: TR)(implicit conn: Conn) {
    checkDbConnectors()
    _original = Some(record)
    key = record._primaryKey
    for (connector <- dbConnectors) connector.load(record)

    loadRecord(record)
    for (formField <- formFieldsWithDb) formField.load(record._primaryKey)

    afterLoadRecord.foreach(_ (record))
    afterLoadRecord2.foreach(_ (AfterLoadRecordCtx(record, conn)))
  }

  //  def loadOrNotFoundAct(id: Int)(implicit conn: Conn, act: Act) {
  //    if (!load(id)) throw ResultException(Results.NotFoundAct)
  //  }
  //  def loadOrNotFoundPage(id: Int)(implicit conn: Conn, page: Page) {
  //    if (!load(id)) throw ResultException(Results.NotFoundPage)
  //  }
  def loadOrNotFoundRaw(id: Int)(implicit conn: Conn): Unit = {
    if (!load(id)) throw ResultException(Results.NotFoundRaw)
  }

  def save()(implicit dt: DataTr): MTR =
    innerSave(None)

  def innerSave(gotChangedItem: Option[(ChangedItemType, Int)])(implicit dt: DataTr): MTR = {
    checkDbConnectors()
    val record: MTR = original.fold(table._newMutableRecord)(_.toMutable.asInstanceOf[MTR])
    val body = {() =>
      beforeSaveRecord.foreach(_ (record))
      beforeSaveRecord2.foreach(_ (BeforeSaveRecordCtx(record, dt)))
      for (connector <- dbConnectors) connector.save(record)
      saveRecord(record)
      val id: Int = original match {
        case Some(o) =>
          base.db.updateChanged(o, record)
          record._primaryKey
        case None =>
          base.db.insert(record).getOrElse(sys.error("Form must have primary key field (id)"))
      }
      val changedItem: Option[(ChangedItemType, Int)] = gotChangedItem.orElse(saveChangedItemType.map(_ -> id))
      // add changed fields
      base.maybeChangedFieldsDao.foreach {dao =>
        for ((tpe, itemId) <- changedItem; orig <- original) {
          val ignoreFields = ignoreToAddChangedFields
          for (field <- table._fields if field.get(orig) != field.getM(record) && !ignoreFields.contains(field))
            dao.insert(tpe, itemId, field, id)
        }
      }

      // save subforms
      for (formField <- formFieldsWithDb) formField.save(id, changedItem)

      afterSaveRecord.foreach(_ (AfterSaveRecordCtx(record, dt)))
      for (connector <- dbConnectors) connector.afterSave(record, original)
      record
    }
    if (wrapSaveRecordFn != null) wrapSaveRecordFn(WrapSaveRecordCtx(body, dt)) else body()
    record
  }

  // ------------------------------- Inner classes -------------------------------

  abstract class customDbConnector[T](val field: Field[T]) extends DbConnector[T, TR, MTR]

  trait DbConnectorStart[T, F <: Field[T]] {
    def builder(block: DbConnectorBuilder[T, F] => Any): F
    def custom(block: F => DbConnector[T, TR, MTR]): F
  }
  class DbConnectorBuilder[T, F <: Field[T]](val field: F) extends DbConnectorStart[T, F] { builder =>
    var loadFn: TR => Any = a => ()
    var saveFn: MTR => Any = a => ()

    override def builder(block: DbConnectorBuilder[T, F] => Any): F = {
      block(this)
      field.dbConnector[TR, MTR](new customDbConnector[T](field) {
        override def load(r: TR): Unit = loadFn(r)
        override def save(r: MTR): Unit = saveFn(r)
      })
    }

    override def custom(block: (F) => DbConnector[T, TR, MTR]): F = field.dbConnector[TR, MTR](block(field))

    def load(fn: TR => Any): this.type = {loadFn = fn; this}
    def save(fn: MTR => Any): this.type = {saveFn = fn; this}
  }


  case class AfterLoadRecordCtx(r: TR, conn: Conn)
  case class BeforeSaveRecordCtx(r: MTR, dt: DataTr)
  case class AfterSaveRecordCtx(r: MTR, dt: DataTr)
  case class WrapSaveRecordCtx(body: () => MTR, dt: DataTr)
}
