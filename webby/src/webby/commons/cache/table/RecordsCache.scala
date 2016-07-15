package webby.commons.cache.table

import querio._

import scala.collection.immutable.IntMap

/**
  * Простой кеш таблицы из БД. Кеширует все записи, поэтому таблица в БД не должна быть очень большой.
  */
trait RecordsCache[TR] extends AbstractRecordsCache {
  protected var _records: IntMap[TR] = null

  /**
    * Вернуть запись по id
    */
  def byId(id: Int): Option[TR] = records.get(id)

  def get(id: Int): Option[TR] = byId(id)

  def apply(id: Int): TR = byId(id).get

  /**
    * Вернуть карту всех записей
    */
  def allRecordsMap: IntMap[TR] = records

  /**
    * Вернуть список всех записей
    */
  def allRecords: Iterable[TR] = records.values

  /**
    * Map содержит запись с заданной id?
    */
  def contains(id: Int): Boolean = records.contains(id)

  def isValidId(id: Int) = contains(id)

  /**
    * Количество всех записей
    */
  def size: Int = records.size

  /**
    * Сбросить весь кеш
    */
  override def resetCache() {
    _records = null
    super.resetCache()
  }

  /**
    * Сбросить значение одной записи, т.е. перечитать кеш для этой записи.
    */
  override def resetRecord(id: Int, change: TrRecordChange) {
    _records match {
      case null =>
      case r =>
        change match {
          case some: TrSomeChange => _records = _records.updated(id, some.mtr.toRecord.asInstanceOf[TR])
          case del: TrDeleteChange => _records = _records - id
          case _ =>
            readRecord(id) match {
              case Some(record) => _records = _records.updated(id, record)
              case None => _records = _records - id
            }
        }
    }
    super.resetRecord(id, change)
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def records: IntMap[TR] = {
    if (_records == null) _records = readAllRecords()
    _records
  }

  protected def readAllRecords(): IntMap[TR]

  protected def readRecord(id: Int): Option[TR]
}
