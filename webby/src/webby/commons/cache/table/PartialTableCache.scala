package webby.commons.cache.table

import querio.{Condition, DbTrait, TableRecord, TrTable}

// TODO: класс недописан, его нельзя использовать
abstract class PartialTableCache[PK, TR <: TableRecord[PK]](db: DbTrait,
                                                            val dbTable: TrTable[PK, TR]) extends RecordsCache[PK, TR] {

  trait View[V] {
    /** Условие для выбора/фильтрации записи. */
    def condition: Condition

    def test(record: TR): Boolean

    def fillAll(records: Iterator[TR])
  }
  protected def makeViews: Iterable[View[_]]
  protected val views: Vector[View[_]] = makeViews.toVector

  /**
    * Получить id из записи.
    */
  protected def idFromRecord(record: TR): PK = record._primaryKey

  protected def readAllRecords(): Map[PK, TR] = {
    val map: Map[PK, TR] = db.query(
      _ select dbTable from dbTable
        where views.map(_.condition).reduce(_ || _)
        fetch()
    ).map(r => idFromRecord(r) -> r)(collection.breakOut)
    for (view <- views) {
      view.fillAll(map.valuesIterator.filter(view.test))
    }
    map
  }

  // TODO: Здесь нужна отдельная реализация resetRecord
  // Возможно, не стоит наследоваться от RecordsCache - тогда нужно разбить тот класс на составляющие.

  protected def readRecord(id: PK): Option[TR] = ???

}
