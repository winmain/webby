package webby.commons.cache.table
import querio.AnyPKTable

/**
  * Один из самых простых кешеров. Хранит один экземпляр [[V]], который высчитывает при первом
  * обращении к нему.
  * При любом изменении таблицы [[dbTable]] сбрасывает кеш хранимого объекта [[V]].
  *
  * Применяется полного или частичного для кеша таблиц, которые редко меняются.
  * В отличие от [[TableCache]] не требует реализовать как получение записи по id, так и получение
  * всех записей.
  *
  * @param dbTable Таблица, любые изменения в которой полностью сбрасывают кеш
  * @param factory Фабрика создания кеша
  * @tparam V Тип хранимого объекта
  */
class WeakTableCache[PK, V](val dbTable: AnyPKTable[PK],
                            factory: => V) extends AbstractRecordsCache[PK] {
  private val cacher: WeakTableCacheValue[V] = new WeakTableCacheValue[V](factory)
  addEventListener(cacher)

  def value: V = cacher.value
}
