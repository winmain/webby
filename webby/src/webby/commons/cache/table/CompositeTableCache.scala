package webby.commons.cache.table
import querio.{CompositeRecord, CompositeTable, Condition, DbTrait}

import scala.collection.immutable.IntMap

/**
  * Кеш для композитных записей.
  * Как правило, это частичный кеш одной таблицы.
  */
trait CompositeTableCache[CR <: CompositeRecord] extends RecordsCache[CR] {

  def db: DbTrait
  def compositeTable: CompositeTable[CR]

  protected override def readAllRecords(): IntMap[CR] = db.query(
    _ select compositeTable from dbTable fetch()).foldLeft(IntMap[CR]())((m, r) => m.updated(idFromRecord(r), r))

  protected override def readRecord(id: Int): Option[CR] = db.query(
    _ select compositeTable from dbTable where recordIdCondition(id) fetchOne())

  // ------------------------------- Abstract methods -------------------------------

  /**
    * Условие для выбора записи по id.
    * Например, table.id == id
    */
  protected def recordIdCondition(id: Int): Condition

  /**
    * Получить id из записи.
    */
  protected def idFromRecord(record: CR): Int
}
