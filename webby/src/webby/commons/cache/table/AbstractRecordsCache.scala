package webby.commons.cache.table
import querio._

trait AbstractRecordsCache[PK] {
  TableCacheMap.register(this)

  protected var listeners: Seq[TableCacheEventListener] = Seq.empty

  /**
    * Таблица, записи которой кешируются.
    */
  def dbTable: AnyPKTable[PK]

  /**
    * Сбросить весь кеш
    */
  def resetCache(): Unit = {
    listeners.foreach(_.onResetCache())
  }

  /**
    * Сбросить значение одной записи, т.е. перечитать кеш для этой записи.
    */
  def resetRecord(id: PK, change: TrRecordChange): Unit = {
    change.validate(dbTable, id)
    listeners.foreach(_.onResetRecord(id, change))
  }

  /**
    * Добавить подписчика на события сброса всего кеша и события сброса кеша одной записи.
    */
  def addEventListener[L <: TableCacheEventListener](listener: L): L = {
    listeners = listeners :+ listener
    listener
  }

  def addResetAnyListener(onResetAny: => Any): TableCacheEventListener = addEventListener(new TableCacheEventListener {
    override def onResetCache(): Unit = onResetAny
    override def onResetRecord(id: Any, change: TrRecordChange): Unit = onResetAny
  })
}
