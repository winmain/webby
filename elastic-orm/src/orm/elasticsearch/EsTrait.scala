package orm.elasticsearch

import java.time.{LocalDate, LocalDateTime}
import java.{lang => jl, util => ju}

import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get._
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.index.query._
import org.elasticsearch.search.{SearchHit, SearchHits}
import querio.{DbEnum, ScalaDbEnum, ScalaDbEnumCls}

import scala.collection.JavaConverters._

/**
 * Trait, реализующий этот trait должен задавать основную конфигурацию класса для индексации Elastic'ом.
 * Этот trait напрямую не используется. Но он нужен для создания реализаций трейтов
 * [[EsRecord]]/[[EsTypeRecord]], [[EsWrite]]/[[EsTypeWrite]], [[EsMeta]]/[[EsTypeMeta]]/[[EsSubTypeMeta]].
 *
 * Существуют основные типы классов:
 * 1. [[EsTrait]] - Описание таблицы Эластика со всеми полями. Его поведение полностью зависит от наследника.
 *                  Он может быть реализован как запись, полученная от Эластика, как запись для индексации
 *                  Эластиком, так и как метаинформация, описывающая все поля таблицы.
 * 2. [[EsRecord]] - Представление записи, полученной от Эластика.
 * 3. [[EsWrite]] - Запись, подготовленная для индексации Эластиком.
 * 4. [[EsMeta]] - Метаинформация об индексе Эластика - список полей, методы для конвертации записей, валидация и прочее.
 * 5. [[EsTypeClient]] - клиент для выполнения запросов Эластику
 *
 * Эти классы имеют типичных наследников с префиксом EsType для головных записей:
 * [[EsTypeTrait]], [[EsTypeRecord]], [[EsTypeWrite]], [[EsTypeMeta]].
 * Только эти записи могут напрямую взаимодействовать с Эластиком, в то время как остальные
 * (без префикса EsType) представляют собой вложенные записи.
 *
 * Для вложенных записей используются классы:
 * [[EsTrait]], [[EsRecord]], [[EsWrite]], [[EsSubTypeMeta]].
 *
 * Иерархия классов следующая:
 * <pre>
 *
 *                               +--------------------- [[EsSubTypeMeta]]
 *                               |
 *              +- [[EsMeta]] ---+--------------------* [[EsTypeMeta]]
 *              |                                    /
 * [[EsTrait]] -+--------------- [[EsTypeTrait]]--*-*
 *              |                                  \ \
 *              +- [[EsBaseRecord]] +- [[EsWrite]] -\-* [[EsTypeWrite]]
 *                                  |                \
 *                                  +- [[EsRecord]] --* [[EsTypeRecord]]
 *
 * [[EsTypeClient]]
 *
 * </pre>
 */
trait EsTrait {
  import scala.language.higherKinds

  /**
   * Тип исходной записи, из которой создаётся запись для Эластика.
   */
  type Record

  /**
   * Волшебный тип, который меняет всё поведение класса. Описывает поля Эластика.
   * Для записей [[EsBaseRecord]], [[EsRecord]], [[EsWrite]] он превращается просто в T.
   * Для метакласса он превращается в описание поля [[EsField]]
   * @tparam T Тип данных для этого поля
   */
  type F[T]

  /**
   * Тип, описывающий список вложенных объектов.
   */
  type FList[C <: EsRecord, M <: EsSubTypeMeta[C]]

  protected def field[A](name: String, fromRecord: Record => A, conv: Conv[A]): F[A]
  protected def objectListField[CC <: EsRecord, M <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[_], meta: M): FList[CC, M]

  protected def commonField[A](name: String, fromRecord: Record => A, conv: SimpleConv[A]): F[A] = field[A](name, fromRecord, new Conv[A] {
    def from(j: AnyRef): A = if (j == null) null.asInstanceOf[A] else conv.from(j)
    def to(v: A): AnyRef = conv.to(v).asInstanceOf[AnyRef]
  })
  protected def optionField[A](name: String, fromRecord: Record => Option[A], conv: SimpleConv[A]): F[Option[A]] = field[Option[A]](name, fromRecord, new Conv[Option[A]] {
    def from(j: AnyRef): Option[A] = if (j == null) None else Some(conv.from(j))
    def to(v: Option[A]): AnyRef = v.map(conv.to(_).asInstanceOf[AnyRef]).orNull
  })

  protected def intField(n: String, fr: Record => Int) = field[Int](n, fr, new AsIs[Int])
  protected def longField(n: String, fr: Record => Long) = field[Long](n, fr, new AsIs[Long])
  protected def doubleField(n: String, fr: Record => Double) = field[Double](n, fr, new AsIs[Double])
  protected def stringField(n: String, fr: Record => String) = field[String](n, fr, new NullableAsIs[String])
  protected def booleanField(n: String, fr: Record => Boolean) = field[Boolean](n, fr, new AsIs[Boolean])
  protected def dateTimeTimestampField(n: String, fr: Record => LocalDateTime) = commonField(n, fr, LocalDateTimeTimestampConv)
  protected def dateMidnightTimestampField(n: String, fr: Record => LocalDate) = commonField(n, fr, LocalDateTimestampConv)
  protected def dateMidnightIntField(n: String, fr: Record => LocalDate, conv: LocalDateIntConv) = commonField(n, fr, conv)

  protected def intOptionField(n: String, fr: Record => Option[Int]) = field[Option[Int]](n, fr, new AsOption[Int])
  protected def longOptionField(n: String, fr: Record => Option[Long]) = field[Option[Long]](n, fr, new AsOption[Long])
  protected def stringOptionField(n: String, fr: Record => Option[String]) = field[Option[String]](n, fr, new AsOption[String])
  protected def dateTimeTimestampOptionField(n: String, fr: Record => Option[LocalDateTime]) = optionField(n, fr, LocalDateTimeTimestampConv)
  protected def dateMidnightTimestampOptionField(n: String, fr: Record => Option[LocalDate]) = optionField(n, fr, LocalDateTimestampConv)
  protected def dateMidnightIntOptionField(n: String, fr: Record => Option[LocalDate], conv: LocalDateIntConv) = optionField(n, fr, conv)

  // ------------------------------- Geo point types -------------------------------
  /** Сейчас здесь мы просто прокидываем строку с числами "lat, lon", так как elastic прекрасно понимает такую запись
    * Но возможно в будущем это поведение изменится,
    * поэтому заводим отдельный метод для типа данных geo_point
    */
  protected def geoPointField(n: String, fr: Record => String) = stringField(n, fr)
  protected def geoPointOptionField(n: String, fr: Record => Option[String]) = stringOptionField(n, fr)

  // ------------------------------- DbEnum fields -------------------------------

  protected def enumField[E <: DbEnum](n: String, fr: Record => E#V, enum: E) = field[E#V](n, fr, new Conv[E#V] {
    /** Чтение значения из ElasticSearch */
    def from(j: AnyRef): E#V = j match {
      case null => null.asInstanceOf[E#V]
      case v: jl.Integer =>
        val obj = enum.getNullable(v.intValue())
        require(obj != null, "No enum " + enum + " for id:" + v)
        obj
    }
    /** Запись значения в ElasticSearch */
    def to(v: E#V): AnyRef = v.getId.asInstanceOf[jl.Integer]
  })
  protected def enumOptionField[E <: DbEnum](n: String, fr: Record => Option[E#V], enum: E) = field[Option[E#V]](n, fr, new Conv[Option[E#V]] {
    /** Чтение значения из ElasticSearch */
    def from(j: AnyRef): Option[E#V] = j match {
      case null => None
      case v: jl.Integer => enum.getValue(v.intValue())
    }
    /** Запись значения в ElasticSearch */
    def to(v: Option[E#V]): AnyRef = v match {
      case None => null
      case Some(e) => e.getId.asInstanceOf[jl.Integer]
    }
  })

  // ------------------------------- Old ScalaDbEnum fields -------------------------------

  protected def enumIdxField[E <: ScalaDbEnumCls[E]](n: String, fr: Record => E, enum: ScalaDbEnum[E]) = field[E](n, fr, new Conv[E] {
    /** Чтение значения из ElasticSearch */
    def from(j: AnyRef): E = j match {
      case null => null.asInstanceOf[E]
      case v: jl.Integer => enum.values(v.intValue())
    }
    /** Запись значения в ElasticSearch */
    def to(v: E): AnyRef = v.index.asInstanceOf[jl.Integer]
  })
  protected def enumIdxOptionField[E <: ScalaDbEnumCls[E]](n: String, fr: Record => Option[E], enum: ScalaDbEnum[E]) = field[Option[E]](n, fr, new Conv[Option[E]] {
    /** Чтение значения из ElasticSearch */
    def from(j: AnyRef): Option[E] = j match {
      case null => None
      case v: jl.Integer => Some(enum.values(v.intValue()))
    }
    /** Запись значения в ElasticSearch */
    def to(v: Option[E]): AnyRef = v match {
      case None => null
      case Some(e) => e.index.asInstanceOf[jl.Integer]
    }
  })
  protected def enumIdxSetField[E <: ScalaDbEnumCls[E]](n: String, fr: Record => Set[E], enum: ScalaDbEnum[E]) = field[Set[E]](n, fr, new Conv[Set[E]] {
    /** Чтение значения из ElasticSearch */
    def from(j: AnyRef): Set[E] = j match {
      case null => Set.empty
      case v: jl.Iterable[_] => v.asScala.map(e => enum.values(e.asInstanceOf[jl.Integer])).toSet
    }
    /** Запись значения в ElasticSearch */
    def to(v: Set[E]): AnyRef =
      if (v.isEmpty) null
      else asJavaCollectionConverter(v.map(_.index))
  })

  // ------------------------------- / -------------------------------

  protected def arrayField[T](n: String, fr: Record => Iterable[T]) = field[Iterable[T]](n, fr, new Conv[Iterable[T]] {
    def from(j: AnyRef): Iterable[T] = if (j == null) Iterable.empty else j.asInstanceOf[jl.Iterable[T]].asScala
    def to(v: Iterable[T]): AnyRef = asJavaIterableConverter(v)
  })
  protected def intArrayField(n: String, fr: Record => Iterable[Int]) = arrayField[Int](n, fr)
  protected def stringArrayField(n: String, fr: Record => Iterable[String]) = arrayField[String](n, fr)

  protected def boostField(fr: Record => Double): F[Double]

  /** Имя поля, содержащего буст (статическая оценка) документа, используемая при текстовом поиске */
  protected def _boostName = "_boost"
  /** Имя поля, содержащего буст (статическая оценка) документа, используемая при поиске фильтром и при сортировке */
  protected def boostName = "boost"
}

/**
 * Сущность, заданная этим трейтом, должна иметь type (отдельную таблицу) в Elastic.
 * Если сущность будет использоваться только как вложенный элемент, то ей достаточно будет наследовать [[EsTrait]]
 */
trait EsTypeTrait extends EsTrait {
  def elasticMapping: ElasticIndexMapping
}

/**
 * Общий базовый класс для [[EsWrite]] и [[EsRecord]]
 */
trait EsBaseRecord extends EsTrait {
  override type F[T] = T
  override type FList[C <: EsRecord, M <: EsSubTypeMeta[C]] = Iterable[C]
}


/**
 * Класс, реализующий этот trait, содержит в себе объект для индексации Elastic'ом.
 * Т.е., это класс для записи в Elastic.
 *
 * Механизм записи реализован так, что все переменные, инициализируемые в [[EsTrait]], вызывают метод #field при инициализации.
 * А этот метод уже получает поле из оригинального объекта (#record), и сохраняет его в специальной карте #valueMap.
 * Таким образом, после создания объекта [[EsWrite]], его #valueMap уже заполнен и готов для записи в Elastic.
 */
trait EsWrite extends EsBaseRecord {
  //  if (!validateRecord(record)) throw new RuntimeException(s"Record $record not valid for creating EsRecord object")

  protected var _valueMap: ju.Map[String, AnyRef] = _
  protected def valueMap: ju.Map[String, AnyRef] = {
    if (_valueMap == null) _valueMap = new ju.HashMap[String, AnyRef]()
    _valueMap
  }
  /** Использовать только для отладки */
  def _unsafeValueMap = valueMap

  protected def record: Record

  protected def field[A](name: String, fromRecord: Record => A, conv: Conv[A]): F[A] = {
    val v = fromRecord(record)
    valueMap.put(name, conv.to(v))
    v
  }
  protected def boostField(fromRecord: Record => Double): F[Double] = {
    val v = fromRecord(record)
    val map = valueMap
    map.put(_boostName, v.asInstanceOf[AnyRef])
    map.put(boostName, v.asInstanceOf[AnyRef])
    v
  }
  protected def objectListField[CC <: EsRecord, M <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[_], meta: M): FList[CC, M] = {
    val list = fromRecord(record)
    valueMap.put(name, asJavaCollectionConverter(list.flatMap {e =>
      val r = e.asInstanceOf[meta.type#Record]
      if (meta.validateRecord(r)) Seq(meta.makeWrite(r).valueMap) else Nil
    }))
    null.asInstanceOf[FList[CC, M]] // Заглушка для объекта. На самом деле, это значение не используется.
  }
}

/**
 * Трейт для индексации записи Elastic'ом.
 */
trait EsTypeWrite extends EsWrite with EsTypeTrait {
  def id: String

  def prepareIndex: IndexRequestBuilder =
    elasticMapping.rawPrepareIndex(id).setSource(valueMap)
}


/**
 * Класс, реализующий этот trait, представляет объект, полученный от Elastic'а.
 * Это может быть как вся запись целиком, так и объект, лежащий внутри главной записи
 * (вложенный объект в терминологии JSON).
 */
trait EsRecord extends EsBaseRecord {
  protected def data: EsClassData

  protected def field[A](name: String, fromRecord: Record => A, conv: Conv[A]): F[A] = {
    try {
      data.getHighlightedField(name) match {
        case null => conv.from(data.get(name))
        case hlt => conv.from(hlt.fragments()(0).string())
      }
    } catch {
      case e: Throwable => throw new RuntimeException(s"Error converting field '$name' from value '${data.get(name)}' in item id:${data.getId}", e)
    }
  }
  protected def boostField(fromRecord: Record => Double): F[Double] = {
    data.get(boostName).asInstanceOf[Double]
  }
  protected def objectListField[CC <: EsRecord, A <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[_], adapter: A): FList[CC, A] = {
    data.get(name) match {
      case null => Vector.empty
      case list => list.asInstanceOf[jl.Iterable[ju.Map[String, AnyRef]]].asScala.map(el => adapter.fromMap(el))
    }
  }

  /** Было ли подсвечено это поле при поиске? Т.е., есть ли там искомые вхождения? */
  def isFieldHighlighted(fieldName: String): Boolean = data.isFieldHighlighted(fieldName)

  /**
   * Если это поле было подсвечено, то вернётся его содержимое. Иначе None.
   */
  def getHighlightedField(fieldName: String): Option[String] = data.getHighlightedField(fieldName) match {
    case null => None
    case hlt => Some(hlt.fragments()(0).string().trim)
  }

  /**
   * Если это поле было подсвечено, то вернётся его содержимое. Иначе None.
   * Также, подставим многоточия в начало и конец, если поле было обрезано.
   * Внимание! Этот метод не работает для вложенных полей, потому что ElasticSearch не даёт возможности определить
   * к какой именно записи внутри объекта принадлежит найденный фрагмент.
   */
  def getHighlightedFieldDecorated(fieldName: String): Option[String] = data.getHighlightedField(fieldName) match {
    case null => None
    case hlt =>
      val fragment = hlt.fragments()(0).string().trim
      val value: String = data.get(fieldName).asInstanceOf[String].trim
      if (fragment.length > value.length) {
        Some(fragment) // Здесь фрагмент целиком показывает текст.
      } else {
        val prependEllipsis = value.substring(0, 10) != fragment.substring(0, 10)
        val appendEllipsis = value.length > 10 && fragment.length > 10 && value.substring(value.length - 10) != fragment.substring(fragment.length - 10)
        if (prependEllipsis || appendEllipsis) {
          Some((if (prependEllipsis) "... " else "") + fragment + (if (appendEllipsis) " ..." else ""))
        } else Some(fragment)
      }
  }
}

/**
 * Главный объект, запись, полученная от Elastic'а.
 * В него могут быть вложены другие объекты [[EsRecord]], но сам он никуда не может входить.
 */
trait EsTypeRecord extends EsRecord {
  def id: String
  def idInt: Int = Integer.parseInt(id)
  def idString: String = id
  def score: Option[Float] = data.score
}

/**
 * Описание поля Elastic'а.
 *
 * @param name Название поля в Эластике
 * @param fromRecord Заполнение этого поля для Эластика из первоначальной записи [[Record]]
 * @param conv Конвертатор для этого поля
 * @tparam T Тип этого поля в Эластике
 * @tparam Record Тип записи, которая используется для заполнения таких полей
 */
class EsField[T, Record](val name: String, val fromRecord: Record => T, conv: Conv[T]) {
  def boostedName(boost: Int): String = if (boost == 1) name else name + "^" + boost

  def termQuery(value: Any): TermQueryBuilder = new TermQueryBuilder(name, value)
  def inQuery(values: Seq[_]): TermsQueryBuilder = new TermsQueryBuilder(name, asJavaIterableConverter(values))
  def rangeQuery: RangeQueryBuilder = new RangeQueryBuilder(name)
  def existsQuery: ExistsQueryBuilder = new ExistsQueryBuilder(name)
}

/**
 * Объект, реализующий этот trait, имеет перечень полей, заданных в [[EsTrait]],
 * а также методы для создания и получения объектов [[EsRecord]] и [[EsTypeRecord]].
 */
trait EsMeta[C <: EsRecord] extends EsTrait {
  override type F[T] = EsField[T, Record]
  override type FList[CC <: EsRecord, M <: EsSubTypeMeta[CC]] = M

  def fieldName: String
  def prefix: String

  protected def field[A](name: String, fromRecord: Record => A, conv: Conv[A]): F[A] =
    new EsField[A, Record](prefix + name, fromRecord, conv)
  protected def boostField(fromRecord: Record => Double): F[Double] =
    new EsField[Double, Record](prefix + boostName, fromRecord, new AsIs[Double])
  protected def objectListField[CC <: EsRecord, M <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[_], meta: M): FList[CC, M] = meta.withPrefix(name, prefix + name + ".").asInstanceOf[FList[CC, M]]

  def validateRecord(r: Record): Boolean
  def makeWrite(r: Record): EsWrite

  /** Проверка: валидна ли эта запись для создания EsWrite из неё? */
  def makeObject(cd: EsClassData): C

  def fromHit(searchHit: SearchHit): C = makeObject(new EsClassHitData(searchHit))
  def fromGet(resp: GetResponse): Option[C] = if (resp.isExists) Some(makeObject(new EsClassGetData(resp))) else None
  def fromGetList(resp: GetResponse): List[C] = if (resp.isExists) List(makeObject(new EsClassGetData(resp))) else Nil
  def fromMap(map: ju.Map[String, AnyRef]): C = makeObject(new EsClassMapData(map))
}

/**
 * [[EsMeta]] для главного объекта.
 */
trait EsTypeMeta[C <: EsTypeRecord] extends EsMeta[C] with EsTypeTrait {
  def makeWrite(r: Record): EsTypeWrite
  def fieldName: String = elasticMapping.tpe
  def prefix: String = ""

  def writeFactory: Record => Option[EsTypeWrite] = (r: Record) => if (validateRecord(r)) Some(makeWrite(r)) else None
}

/**
 * [[EsMeta]] для вложенного объекта.
 */
trait EsSubTypeMeta[C <: EsRecord] extends EsMeta[C] {
  def withPrefix(name: String, prefix: String): EsSubTypeMeta[C]
}


/**
 * Трейт содержит в себе методы для работы с клиентом эластика.
 */
trait EsTypeClient[C <: EsTypeRecord] {

  def meta: EsTypeMeta[C]

  def em: ElasticIndexMapping = meta.elasticMapping

  // ------------------------------- Private & protected methods -------------------------------

  protected def searchResponse(search: Object, response: SearchResponse): EsResult[C] = {
    val hits: SearchHits = response.getHits

    new EsResult[C](took = response.getTookInMillis,
      found = hits.totalHits().toInt,
      rows = hits.hits().map(meta.fromHit)(scala.collection.breakOut),
      aggs = Option(response.getAggregations),
      esQueryBuilder = search)
  }

  /**
   * Замерить время выполнения запроса, который не возвращает затраченное время, и выполнить этот
   * запрос. Полученное время учитывается в статистике вызовом метода [[statAddOne()]]
   */
  protected def statByRealTime[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result: R = block
    val millis = System.currentTimeMillis() - t0
    statAddOne(millis)
    result
  }

  /**
   * Получить время выполнения запроса из результата [[EsResult]] и учесть это время в статистике
   * вызовом метода [[statAddOne()]].
   */
  protected def stat(result: EsResult[C]): EsResult[C] = { statAddOne(result.took); result }
  protected def stat(result: SearchResponse): SearchResponse = { statAddOne(result.getTookInMillis); result }

  /**
   * Учесть время, выполненное этим запросом.
   */
  protected def statAddOne(millis: Long): Unit = {}

  // ------------------------------- Public methods -------------------------------

  def delete(id: String)(block: DeleteRequestBuilder => Any): DeleteResponse =
    statByRealTime(em.delete(id)(block))

  def count(block: SearchRequestBuilder => Any): Long =
    stat(em.search(s => block(s.setSize(0)))).getHits.getTotalHits

  def getRaw(id: String)(block: GetRequestBuilder => Any): GetResponse =
    statByRealTime(em.get(id)(block))

  def get(id: String): Option[C] =
    meta.fromGet(statByRealTime(em.get(id)(a => a)))

  def getById(id: Int): Option[C] = get(id.toString)
  def getById(id: String): Option[C] = get(id)

  def multiGetRaw(ids: Iterable[String]): MultiGetResponse = statByRealTime(em.multiGet(
    _.add(em.index, em.tpe, ids.asJava)
  ))

  def multiGet(ids: Iterable[String]): EsResult[C] = {
    var search: MultiGetRequestBuilder = null
    val response = statByRealTime(em.multiGet {b =>
      b.add(em.index, em.tpe, ids.asJava)
      search = b
    })

    val hits: Array[MultiGetItemResponse] = response.getResponses
    val rows: Vector[C] = hits.flatMap(r => meta.fromGetList(r.getResponse))(scala.collection.breakOut)

    new EsResult[C](took = 0,
      found = rows.size,
      rows = rows,
      aggs = None,
      esQueryBuilder = search)
  }

  def searchRaw(block: SearchRequestBuilder => Any): SearchResponse =
    stat(em.search(block))

  def search(block: SearchRequestBuilder => Any): EsResult[C] = {
    var search: SearchRequestBuilder = null
    val resp = em.search {b =>
      block(b)
      search = b
    }
    stat(searchResponse(search, resp))
  }

  def moreLikeThisItem(id: String): MoreLikeThisQueryBuilder.Item = new MoreLikeThisQueryBuilder.Item(em.index, em.tpe, id)
  def moreLikeThisItem(id: Int): MoreLikeThisQueryBuilder.Item = moreLikeThisItem(id.toString)

  /** Внимание. Если элемент с указанным id не будет найден, то будет брошен DocumentMissingException */
  def moreLikeThis(id: String)(block: MoreLikeThisQueryBuilder => Any): EsResult[C] = {
    val mlt: MoreLikeThisQueryBuilder = moreLikeThisQuery().addLikeItem(new Item(em.index, em.tpe, id))
    var search: SearchRequestBuilder = null
    val resp = em.search {b =>
      block(mlt)
      b.setQuery(mlt)
      search = b
    }
    stat(searchResponse(search, resp))
  }
}
