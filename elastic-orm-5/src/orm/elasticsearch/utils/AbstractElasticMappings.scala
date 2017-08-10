package orm.elasticsearch.utils

import org.elasticsearch.action.index.IndexResponse
import orm.elasticsearch._
import webby.commons.collection.IterableWrapper.wrapIterable
import querio.{DbTrait, TableRecord, TrTable}

import scala.concurrent.Future

/**
  * Карта всех связей таблиц БД с таблицами ElasticSearch.
  *
  * Для использования этого класса нужно создать объект из его наследника.
  * Пример:
  * {{{
  *   object ElasticMap extends AbstractElasticMappings {
  *     override protected def db: DbTrait = Db
  *
  *     private val vac = Mapping[Vac](Vac, VacDao.findVisibleById, EsVac.writeFactory, EsVacClientNoStat)
  *     private val res = Mapping[Res2](Res2, ResDao.findByIdForEs, EsRes.writeFactory, EsResClientNoStat)
  *
  *     override val allMappings: Vector[Mapping[_ <: TableRecord]] = Vector(vac, res)
  *
  *     override val relatedMappings: Vector[RelatedMapping[_ <: TableRecord]] = Vector(
  *       res.related(Res2Job)(_.resId),
  *       res.related(Res2Exp)(_.resId)
  *     )
  *   }
  * }}}
  */
abstract class AbstractElasticMappings(mapList: ElasticMapListTrait) {

  import ElasticMappings._

  // --------------------------- Fields, methods, classes ---------------------------

  val mappingsMap: Map[String, Mapping[_ <: TableRecord]] = {
    mapList.allMappings.mapToMap[String, Mapping[_ <: TableRecord]](m => m.table._fullTableName -> m)
  }

  val allMap: Map[String, Reindexable[_ <: TableRecord]] =
    ((mapList.allMappings: Vector[Reindexable[_ <: TableRecord]]) ++ mapList.relatedMappings)
      .mapToMap[String, Reindexable[_ <: TableRecord]](m => m.table._fullTableName -> m)


  /** Получить Mapping по его полному имени, например ros.tag */
  def get(fullName: String): Option[Reindexable[_ <: TableRecord]] = allMap.get(fullName)
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
    * @param db                 Соединение с БД
    * @param table              Таблица БД
    * @param getByIdForIndex    Получение записи по id для индексации. Здесь можно первоначально проверить, годится ли запись к индексации
    * @param writeFactory       Фабрика создания EsTypeWrite из TableRecord для индексации
    * @param esTypeClientNoStat Клиент эластика с выключенной статистикой, потому что он будет использоваться для удаления записей.
    */
  case class Mapping[TR <: TableRecord](db: DbTrait,
                                        table: TrTable[TR],
                                        getByIdForIndex: Int => Option[TR],
                                        writeFactory: TR => Option[EsTypeWrite],
                                        esTypeClientNoStat: EsTypeClient[_ <: EsTypeRecord]) extends Reindexable[TR] {
    def reindexRecord(id: Int): Option[Future[IndexResponse]] =
      (for (tr <- getByIdForIndex(id);
            record <- writeFactory(tr))
        yield record) match {
        case Some(record) => Some(Future(ElasticSearch.executeAndGet(record.prepareIndex)))
        case None => esTypeClientNoStat.delete(id.toString)(a => a); None
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
