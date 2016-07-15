package webby.commons.cache.table

import querio.{Condition, DbTrait, TableRecord, TrTable}
import webby.commons.collection.IterableWrapper.wrapIterable

import scala.collection.immutable.IntMap

// TODO: класс недописан, его нельзя использовать
abstract class PartialTableCache[TR <: TableRecord](db: DbTrait, val dbTable: TrTable[TR]) extends RecordsCache[TR] {

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
  protected def idFromRecord(record: TR): Int = record._primaryKey

  protected def readAllRecords(): IntMap[TR] = {
    val map: IntMap[TR] = db.query(
      _ select dbTable from dbTable
        where views.map(_.condition).reduce(_ || _)
        fetch()).mapToIntMap(r => idFromRecord(r) -> r)
    for (view <- views) {
      view.fillAll(map.valuesIterator.filter(view.test))
    }
    map
  }

  // TODO: Здесь нужна отдельная реализация resetRecord
  // Возможно, не стоит наследоваться от RecordsCache - тогда нужно разбить тот класс на составляющие.

  protected def readRecord(id: Int): Option[TR] = ???

}
