package webby.commons.cache.table

import querio._

import scala.collection.immutable.IntMap


/**
  * Стандартный кеш для таблицы #dbTable
  */
class TableCache[TR <: TableRecord](db: DbTrait, val dbTable: TrTable[TR]) extends RecordsCache[TR] {

  protected def readAllRecords(): IntMap[TR] = db.query(
    _ selectFrom dbTable fetch()).foldLeft(IntMap[TR]())((m, r) => m.updated(idFromRecord(r), r))

  protected def readRecord(id: Int): Option[TR] = db.query(
    _ selectFrom dbTable where recordIdCondition(id) fetchOne())

  /**
    * Условие для выбора записи по id.
    */
  protected def recordIdCondition(id: Int): Condition = dbTable._primaryKey.get == id

  /**
    * Получить id из записи.
    */
  protected def idFromRecord(record: TR): Int = record._primaryKey
}

