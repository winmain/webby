package webby.commons.cache.table
import querio._

/**
  * Слушатель событий изменения кешированной таблицы
  */
trait TableCacheEventListener {
  def onResetCache() {}
  def onResetRecord(id: Int, change: TrRecordChange) {}
}
