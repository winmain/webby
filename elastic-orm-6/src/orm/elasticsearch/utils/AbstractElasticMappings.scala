package orm.elasticsearch.utils

import org.elasticsearch.action.index.IndexResponse
import orm.elasticsearch._
import querio.{DbTrait, TableRecord, TrTable}
import webby.commons.collection.IterableWrapper.wrapIterable

import scala.collection.mutable
import scala.concurrent.Future

/**
  * Map for all links between DB tables and Elastic tables.
  *
  * Example usage:
  * {{{
  * object ElasticMapList extends ElasticMapListTrait {
  *   import ElasticMappings._
  *
  *   private val cat = Mapping[Cat](Db, Cat, CatDao.findVisibleById, EsCat.writeFactory, EsCatClientNoStat)
  *   private val dog = Mapping[Dog](Db, Dog, DogDao.findByIdForEs, EsDog.writeFactory, EsDogClientNoStat)
  *
  *   override def allMappings: Vector[Mapping[_ <: TableRecord]] = Vector(cat, dog)
  *
  *   override def relatedMappings: Vector[RelatedMapping[_ <: TableRecord]] = Vector(
  *     cat.related(Cat2Color)(_.catId),
  *     cat.related(Cat2House)(_.catId)
  *   )
  *
  * object ElasticMap extends AbstractElasticMappings(ElasticMapList)
  *
  *
  * object DbHooks {
  *   def resetRecordCache(tableName: String, id: Int, change: TrRecordChange, propagateServers: Boolean) {
  *     ...
  *     ElasticMap.get(tableName).foreach(_.reindexRecord(id))
  *     ...
  *   }
  * }
  * }}}
  */
abstract class AbstractElasticMappings(mapList: ElasticMapListTrait) {

  import ElasticMappings._

  // --------------------------- Fields, methods, classes ---------------------------

  val mappingsMap: Map[String, Mapping[_ <: TableRecord]] = {
    mapList.allMappings.mapToMap[String, Mapping[_ <: TableRecord]](m => m.table._fullTableName -> m)
  }

  val allMap: Map[String, Seq[Reindexable[_ <: TableRecord]]] = {
    val map = mutable.Map[String, List[Reindexable[_ <: TableRecord]]]()

    def addToMap(m: Reindexable[_ <: TableRecord]): Unit = {
      val key: String = m.table._fullTableName
      map.put(key, m :: map.getOrElse(key, Nil))
    }

    mapList.allMappings.foreach(addToMap)
    mapList.relatedMappings.foreach(addToMap)
    map.toMap
  }

  /** Получить Mapping по его полному имени, например ros.tag */
  def get(fullName: String): Seq[Reindexable[_ <: TableRecord]] = allMap.getOrElse(fullName, Nil)
}


trait ElasticMapListTrait {
  // --------------------------- Mapping list ---------------------------

  /**
    * Список всех маппингов таблиц [[querio.Table]] на классы [[EsTrait]]
    */
  def allMappings: Vector[ElasticMappings.Mapping[_ <: TableRecord]]

  /**
    * Маппинги подтаблиц на их родительские таблицы. Они работают так, что при изменении записи
    * в подтаблице, родительская таблица обновляет кеш.
    */
  def relatedMappings: Vector[ElasticMappings.RelatedMapping[_ <: TableRecord]] = Vector.empty
}


object ElasticMappings {
  import scala.concurrent.ExecutionContext.Implicits.global

  trait Reindexable[TR <: TableRecord] {
    def table: TrTable[TR]

    /**
      * Переиндексировать, либо удалить запись из индекса (если её нет, или она не годится для индексации).
      */
    def reindexRecord(id: Int): Option[Future[IndexResponse]]
  }

  /**
    * Прямой mapping таблицы для индексации ElasticSearch.
    *
    * @param db                  Соединение с БД
    * @param table               Таблица БД
    * @param getByIdForIndex     Получение записи по id для индексации. Здесь можно первоначально проверить, годится ли запись к индексации
    * @param writeFactory        Фабрика создания EsTypeWrite из TableRecord для индексации
    * @param esTypeClientsNoStat Клиенты эластика с выключенной статистикой для удаления несуществующих записей.
    */
  case class Mapping[TR <: TableRecord](db: DbTrait,
                                        table: TrTable[TR],
                                        getByIdForIndex: Int => Option[TR],
                                        writeFactory: TR => Option[EsTypeWrite],
                                        esTypeClientsNoStat: Seq[EsTypeClient[_ <: EsTypeRecord]]) extends Reindexable[TR] {
    def reindexRecord(id: Int): Option[Future[IndexResponse]] =
      (for (tr <- getByIdForIndex(id);
            record <- writeFactory(tr))
        yield record) match {
        case Some(record) => Some(Future(ElasticSearch.executeAndGet(record.prepareIndex)))
        case None => esTypeClientsNoStat.foreach(_.delete(id.toString)(_ => ())); None
      }

    /**
      * Shortcut для создания RelatedMapping
      */
    def related[R <: TableRecord](table: TrTable[R])(getMainId: R => Int): RelatedMapping[R] =
      RelatedMapping[R](this, table, id => db.findById(table, id).map(getMainId))
  }

  /**
    * Косвенный mapping, когда изменение подчинённой таблицы вызывает переиндексацию основной записи.
    * Например, это может быть таблица res_tag, для которой основная таблица - res.
    */
  case class RelatedMapping[TR <: TableRecord](mainMapping: Mapping[_ <: TableRecord],
                                               table: TrTable[TR],
                                               getMainIdForIndex: Int => Option[Int]) extends Reindexable[TR] {
    def reindexRecord(id: Int): Option[Future[IndexResponse]] = getMainIdForIndex(id).flatMap(mainMapping.reindexRecord)
  }
}
