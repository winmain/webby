package webby.commons.cache.table

import querio._


/**
  * Стандартный кеш для таблицы #dbTable
  */
class TableCache[TR <: TableRecord](db: DbTrait, val dbTable: TrTable[TR]) extends RecordsCache[TR] {

  protected def readAllRecords(): Map[Int, TR] = {
    val builder = Map.newBuilder[Int, TR]
    db.query(
      _ selectFrom dbTable fetchLazy {it =>
        it.foreach(r => builder += ((idFromRecord(r), r)))
      })
    builder.result()
  }

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

