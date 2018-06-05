package webby.commons.cache.table
import querio.TrRecordChange

/**
  * Обёртка для создания простого объекта, который пересоздаётся каждый раз при изменении любого значения в таблице.
  *
  * Может использоваться отдельно от [[WeakTableCache]].
  */
class WeakTableCacheValue[V](factory: => V) extends TableCacheEventListener {

  @volatile private var initialized = false
  private var _value: V = _

  def value: V = {
    if (!initialized) {
      synchronized {
        if (!initialized) {
          _value = factory
          initialized = true
        }
      }
    }
    _value
  }

  override def onResetCache() {
    initialized = false
  }

  override def onResetRecord(id: Any, change: TrRecordChange): Unit = {
    initialized = false
  }
}
