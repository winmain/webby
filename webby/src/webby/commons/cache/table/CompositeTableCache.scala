package webby.commons.cache.table
import querio.{CompositeRecord, CompositeTable, Condition, DbTrait}

/**
  * Кеш для композитных записей.
  * Как правило, это частичный кеш одной таблицы.
  */
trait CompositeTableCache[PK, CR <: CompositeRecord] extends RecordsCache[PK, CR] {

  def db: DbTrait
  def compositeTable: CompositeTable[CR]

  protected override def readAllRecords(): Map[PK, CR] =
    db.query(_ select compositeTable from dbTable fetch())
      .map(r => idFromRecord(r) -> r)(collection.breakOut)

  protected override def readRecord(id: PK): Option[CR] =
    db.query(_ select compositeTable from dbTable where recordIdCondition(id) fetchOne())

  // ------------------------------- Abstract methods -------------------------------

  /**
    * Условие для выбора записи по id.
    * Например, table.id == id
    */
  protected def recordIdCondition(id: PK): Condition

  /**
    * Получить id из записи.
    */
  protected def idFromRecord(record: CR): PK
}
