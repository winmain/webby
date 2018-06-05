package webby.commons.cache.table

import querio._


/**
  * Стандартный кеш для таблицы #dbTable
  */
class TableCache[PK, TR <: TableRecord[PK]](db: DbTrait,
                                            val dbTable: TrTable[PK, TR]) extends RecordsCache[PK, TR] {

  protected def readAllRecords(): Map[PK, TR] = {
    val builder = Map.newBuilder[PK, TR]
    db.query(
      _ selectFrom dbTable fetchLazy {it =>
        it.foreach(r => builder += ((idFromRecord(r), r)))
      })
    builder.result()
  }

  protected def readRecord(id: PK): Option[TR] = db.query(
    _ selectFrom dbTable where recordIdCondition(id) fetchOne())

  /**
    * Условие для выбора записи по id.
    */
  protected def recordIdCondition(id: PK): Condition = dbTable._primaryKey.get == id

  /**
    * Получить id из записи.
    */
  protected def idFromRecord(record: TR): PK = record._primaryKey
}

