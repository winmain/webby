package webby.commons.cache.table

/**
  * Карта всех созданных TableCache'ов
  */
object TableCacheMap {
  private var map = Map[String, Set[AbstractRecordsCache]]()

  def register(tc: AbstractRecordsCache) {
    val key: String = tc.dbTable._fullTableName
    map = map.get(key) match {
      case Some(set) => map.updated(key, set + tc)
      case None => map.updated(key, Set(tc))
    }
  }

  /** Получить TableCache по его полному имени, например ros.tag */
  def get(fullName: String): Set[AbstractRecordsCache] = map.get(fullName).getOrElse(Set.empty)
}
