package webby.commons.cache.table

/**
  * Карта всех созданных TableCache'ов
  */
object TableCacheMap {
  private var map = Map[String, Set[AbstractRecordsCache[_]]]()

  def register(tc: AbstractRecordsCache[_]) {
    val key: String = tc.dbTable._fullTableName
    map = map.get(key) match {
      case Some(set) => map.updated(key, set + tc)
      case None => map.updated(key, Set(tc))
    }
  }

  /** Получить TableCache по его полному имени, например ros.tag */
  def get(fullName: String): Set[AbstractRecordsCache[_]] = map.get(fullName).getOrElse(Set.empty)
}
