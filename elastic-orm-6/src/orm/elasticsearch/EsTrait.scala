package orm.elasticsearch

import java.time.{Instant, LocalDate, LocalDateTime}
import java.{lang => jl, util => ju}

import enumeratum.values.{IntEnum, IntEnumEntry}
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get._
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.common.geo.GeoPoint
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item
import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.index.query._
import org.elasticsearch.search.{SearchHit, SearchHits}
import querio.DbEnum

import scala.collection.mutable

// for Scala 2.11: import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/*
Live template for Intellij IDEA, named "elasticorm_estypetrait"
--------------
trait $CLASS_NAME$Trait extends EsTypeTrait {
  override type Record = $TABLE_NAME$
  override def elasticMapping: ElasticIndexMapping = Elastic$CLASS_NAME$

  val id = idField(r => String.valueOf(r.id))
  $END$
}

class $CLASS_NAME$Write(val record: $TABLE_NAME$) extends $CLASS_NAME$Trait with EsTypeWrite

class $CLASS_NAME$(protected val data: EsClassData) extends $CLASS_NAME$Trait with EsTypeRecord

class $CLASS_NAME$Meta extends $CLASS_NAME$Trait with EsTypeMeta[$CLASS_NAME$] {
  override def validateRecord(r: $TABLE_NAME$): Boolean = true
  override def makeWrite(r: $TABLE_NAME$): EsTypeWrite = new $CLASS_NAME$Write(r)
  override def makeObject(cd: EsClassData): $CLASS_NAME$ = new $CLASS_NAME$(cd)
}

object $CLASS_NAME$ extends $CLASS_NAME$Meta

// ------------------------------- Clients -------------------------------

class Base$CLASS_NAME$Client extends EsTypeClient[$CLASS_NAME$] {
  override def meta: EsTypeMeta[$CLASS_NAME$] = $CLASS_NAME$
}
object $CLASS_NAME$ClientNoStat extends Base$CLASS_NAME$Client

object $CLASS_NAME$Client extends Base$CLASS_NAME$Client with EsTypeClientWithStat[$CLASS_NAME$] {
  override protected def elasticStatDim: ElasticStat.Dim = ElasticStat.Dimension.$DIMENSION$
}
--------------


Live template for Intellij IDEA, named "elasticorm_essubtypetrait"
--------------
trait $CLASS_NAME$Trait extends EsTrait {
  override type Record = $TABLE_NAME$

  $END$
}

class $CLASS_NAME$Write(val record: $TABLE_NAME$) extends $CLASS_NAME$Trait with EsWrite

class $CLASS_NAME$(protected val data: EsClassData) extends $CLASS_NAME$Trait with EsRecord

class $CLASS_NAME$Meta(val fieldName: String, val path: String) extends $CLASS_NAME$Trait with EsSubTypeMeta[$CLASS_NAME$] {
  override def validateRecord(r: $TABLE_NAME$): Boolean = true
  override def makeWrite(r: $TABLE_NAME$): EsWrite = new $CLASS_NAME$Write(r)
  override def makeObject(cd: EsClassData): $CLASS_NAME$ = new $CLASS_NAME$(cd)
  override def withPath(name: String, path: String): EsSubTypeMeta[$CLASS_NAME$] = new $CLASS_NAME$Meta(name, path)
}

object $CLASS_NAME$ extends $CLASS_NAME$Meta("", "")
--------------
 */

/**
  * Trait, реализующий этот trait должен задавать основную конфигурацию класса для индексации Elastic'ом.
  * Этот trait напрямую не используется. Но он нужен для создания реализаций трейтов
  * [[EsRecord]]/[[EsTypeRecord]], [[EsWrite]]/[[EsTypeWrite]], [[EsMeta]]/[[EsTypeMeta]]/[[EsSubTypeMeta]].
  *
  * Существуют основные типы классов:
  * 1. [[EsTrait]] - Описание таблицы Эластика со всеми полями. Его поведение полностью зависит от наследника.
  * Он может быть реализован как запись, полученная от Эластика, как запись для индексации
  * Эластиком, так и как метаинформация, описывающая все поля таблицы.
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
  * +-------------- [[EsSubTypeMeta]]
  * |
  * +- [[EsMeta]] -------------------------+--- [[EsTypeMeta]]
  * |                                      |
  * [[EsTrait]] ------------------------ [[EsTypeTrait]]
  * |                                      |
  * +- [[EsBaseRecord]] -- [[EsWrite]] ----+--- [[EsTypeWrite]]
  * |
  * +------- [[EsRecord]] ------- [[EsTypeRecord]]
  *
  * [[EsTypeClient]]
  *
  * </pre>
  *
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
    * @tparam R Тип данных для этого поля
    */
  type F[R]

  /**
    * Тип, описывающий список вложенных объектов.
    */
  type FList[C <: EsRecord, M <: EsSubTypeMeta[C]]

  protected def idField(fr: Record => String): F[String]
  protected def field[W, R](name: String, fromRecord: Record => W, conv: Conv[W, R]): F[R]
  protected def objectListField[CC <: EsRecord, M <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[M#Record], meta: M): FList[CC, M]

  protected def field1[A](name: String, fromRecord: Record => A, conv: Conv[A, A]): F[A] = field[A, A](name, fromRecord, conv)

  protected def commonField[A](name: String, fromRecord: Record => A, conv: SimpleConv[A]): F[A] = field1[A](name, fromRecord, new ConvOne[A] {
    override def from(j: AnyRef): A = if (j == null) null.asInstanceOf[A] else conv.from(j)
    override def to(v: A): AnyRef = conv.to(v).asInstanceOf[AnyRef]
  })
  protected def optionField[A](name: String, fromRecord: Record => Option[A], conv: SimpleConv[A]): F[Option[A]] = field1[Option[A]](name, fromRecord, new ConvOne[Option[A]] {
    override def from(j: AnyRef): Option[A] = if (j == null) None else Some(conv.from(j))
    override def to(v: Option[A]): AnyRef = v.map(conv.to(_).asInstanceOf[AnyRef]).orNull
  })

  // ------------------------------- Common fields -------------------------------

  protected def intField(n: String, fr: Record => Int) = field1[Int](n, fr, new AsIs[Int, Int])
  protected def longField(n: String, fr: Record => Long) = field1[Long](n, fr, LongConv)
  protected def doubleField(n: String, fr: Record => Double) = field1[Double](n, fr, new AsIs[Double, Double])
  protected def stringField(n: String, fr: Record => String) = field1[String](n, fr, new NullableAsIs[String, String])
  protected def booleanField(n: String, fr: Record => Boolean) = field1[Boolean](n, fr, new AsIs[Boolean, Boolean])

  /** This field requires {type: "date", format: "epoch_millis"} or {type: "long"} */
  protected def dateTimeTimestampField(n: String, fr: Record => LocalDateTime) = commonField(n, fr, LocalDateTimeTimestampConv)
  /** This field requires {type: "date", format: "epoch_millis"} or {type: "long"} */
  protected def dateMidnightTimestampField(n: String, fr: Record => LocalDate) = commonField(n, fr, LocalDateTimestampConv)
  protected def dateMidnightIntField(n: String, fr: Record => LocalDate, conv: LocalDateIntConv) = commonField(n, fr, conv)
  /** This field requires {type: "date", format: "epoch_millis"} or {type: "long"} */
  protected def instantField(n: String, fr: Record => Instant) = commonField(n, fr, InstantEpochMillisConv)

  protected def objectField(n: String, fr: Record => ju.Map[String, Any]) = field1[ju.Map[String, Any]](n, fr, new AsIs)
  /** This field like [[objectField()]], but it never returns null. So you don't need to bother with null checks. Also, your empty map will always be null in elastic. */
  protected def objectEmptyAsNullField(n: String, fr: Record => ju.Map[String, Any]) = field1[ju.Map[String, Any]](n, fr, new EmptyMapAsNull)

  // ------------------------------- Option fields -------------------------------

  protected def intOptionField(n: String, fr: Record => Option[Int]) = field1[Option[Int]](n, fr, new AsOption[Int, Int])
  protected def longOptionField(n: String, fr: Record => Option[Long]) = field1[Option[Long]](n, fr, new AsOption[Long, Long])
  protected def doubleOptionField(n: String, fr: Record => Option[Double]) = field1[Option[Double]](n, fr, new AsOption[Double, Double])
  protected def stringOptionField(n: String, fr: Record => Option[String]) = field1[Option[String]](n, fr, new AsOption[String, String])
  /** This field requires {type: "date", format: "epoch_millis"} or {type: "long"} */
  protected def dateTimeTimestampOptionField(n: String, fr: Record => Option[LocalDateTime]) = optionField(n, fr, LocalDateTimeTimestampConv)
  /** This field requires {type: "date", format: "epoch_millis"} or {type: "long"} */
  protected def dateMidnightTimestampOptionField(n: String, fr: Record => Option[LocalDate]) = optionField(n, fr, LocalDateTimestampConv)
  protected def dateMidnightIntOptionField(n: String, fr: Record => Option[LocalDate], conv: LocalDateIntConv) = optionField(n, fr, conv)
  /** This field requires {type: "date", format: "epoch_millis"} or {type: "long"} */
  protected def instantOptionField(n: String, fr: Record => Option[Instant]) = optionField(n, fr, InstantEpochMillisConv)

  protected def objectOptionField(n: String, fr: Record => Option[ju.Map[String, Any]]) = field1[Option[ju.Map[String, Any]]](n, fr, new AsOption)

  // ------------------------------- Geo point fields -------------------------------
  protected def geoPointField(n: String, fr: Record => GeoPoint) = commonField[GeoPoint](n, fr, GeoPointConv)
  protected def geoPointOptionField(n: String, fr: Record => Option[GeoPoint]) = optionField[GeoPoint](n, fr, GeoPointConv)

  // ------------------------------- DbEnum fields -------------------------------

  protected def dbEnumField[E <: DbEnum](n: String, enum: E)(fr: Record => E#V) = field1[E#V](n, fr, new ConvOne[E#V] {
    override def from(j: AnyRef): E#V = j match {
      case null => null.asInstanceOf[E#V]
      case v: jl.Integer =>
        val obj = enum.getNullable(v.intValue())
        require(obj != null, "No enum " + enum + " for id:" + v)
        obj
    }

    override def to(v: E#V): AnyRef = v.getId.asInstanceOf[jl.Integer]
  })

  protected def dbEnumOptionField[E <: DbEnum](n: String, enum: E)(fr: Record => Option[E#V]) = field1[Option[E#V]](n, fr, new ConvOne[Option[E#V]] {
    override def from(j: AnyRef): Option[E#V] = j match {
      case null => None
      case v: jl.Integer => enum.getValue(v.intValue())
    }

    override def to(v: Option[E#V]): AnyRef = v match {
      case None => null
      case Some(e) => e.getId.asInstanceOf[jl.Integer]
    }
  })

  protected def dbEnumSetField[E <: DbEnum](n: String, enum: E)(fr: Record => Set[E#V]) = field1[Set[E#V]](n, fr, new ConvOne[Set[E#V]] {
    override def from(j: AnyRef): Set[E#V] = j match {
      case null => Set.empty
      case v: jl.Iterable[_] =>
        val sb = Set.newBuilder[E#V]
        v.forEach {case id: jl.Integer => enum.getValue(id).foreach(sb += _)}
        sb.result()
    }

    override def to(v: Set[E#V]): AnyRef =
      if (v.isEmpty) null
      else asJavaCollection(v.map(_.getId))
  })

  // ------------------------------- Enumeratum fields -------------------------------

  protected def intEnumField[EE <: IntEnumEntry](n: String, enum: IntEnum[EE])(fr: Record => EE) = field1[EE](n, fr, new ConvOne[EE] {
    override def from(j: AnyRef): EE = j match {
      case null => null.asInstanceOf[EE]
      case v: jl.Integer => enum.withValue(v.intValue())
    }

    override def to(v: EE): AnyRef = v.value.asInstanceOf[jl.Integer]
  })

  protected def intEnumOptionField[EE <: IntEnumEntry](n: String, enum: IntEnum[EE])(fr: Record => Option[EE]) = field1[Option[EE]](n, fr, new ConvOne[Option[EE]] {
    override def from(j: AnyRef): Option[EE] = j match {
      case null => None
      case v: jl.Integer => enum.withValueOpt(v.intValue())
    }

    override def to(v: Option[EE]): AnyRef = v match {
      case None => null
      case Some(e) => e.value.asInstanceOf[jl.Integer]
    }
  })

  protected def intEnumSetField[EE <: IntEnumEntry](n: String, enum: IntEnum[EE])(fr: Record => Set[EE]) = field1[Set[EE]](n, fr, new ConvOne[Set[EE]] {
    override def from(j: AnyRef): Set[EE] = j match {
      case null => Set.empty
      case v: jl.Iterable[_] =>
        val sb = Set.newBuilder[EE]
        v.forEach {case id: jl.Integer => enum.withValueOpt(id).foreach(sb += _)}
        sb.result()
    }

    override def to(v: Set[EE]): AnyRef =
      if (v.isEmpty) null
      else asJavaCollection(v.map(_.value))
  })

  // ------------------------------- Array fields -------------------------------

  protected def arrayIterableField[T](n: String, fr: Record => Iterable[T]) = field1[Iterable[T]](n, fr, new ConvOne[Iterable[T]] {
    override def from(j: AnyRef): Iterable[T] = if (j == null) Iterable.empty else iterableAsScalaIterable(j.asInstanceOf[jl.Iterable[T]])
    override def to(v: Iterable[T]): AnyRef = asJavaIterable(v)
  })
  protected def intIterableField(n: String, fr: Record => Iterable[Int]) = arrayIterableField[Int](n, fr)
  protected def stringIterableField(n: String, fr: Record => Iterable[String]) = arrayIterableField[String](n, fr)

  protected def arrayBufferField[T](n: String, fr: Record => Iterable[T]) = field[Iterable[T], mutable.Buffer[T]](n, fr, new Conv[Iterable[T], mutable.Buffer[T]] {
    override def from(j: AnyRef): mutable.Buffer[T] = if (j == null) mutable.Buffer.empty else asScalaBuffer(j.asInstanceOf[ju.List[T]])
    override def to(v: Iterable[T]): AnyRef = asJavaIterable(v)
  })
  protected def intBufferField(n: String, fr: Record => Iterable[Int]) = arrayBufferField[Int](n, fr)
  protected def stringBufferField(n: String, fr: Record => Iterable[String]) = arrayBufferField[String](n, fr)

  // ------------------------------- / -------------------------------

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
  protected var _valueMap: ju.Map[String, AnyRef] = _
  protected def valueMap: ju.Map[String, AnyRef] = {
    if (_valueMap == null) _valueMap = new ju.HashMap[String, AnyRef]()
    _valueMap
  }
  /** Использовать только для отладки */
  def _unsafeValueMap = valueMap

  protected def record: Record

  override protected def idField(fromRecord: (Record) => String): String = {
    val id = fromRecord(record)
    require(id != null, "Id cannot be null")
    id
  }
  override protected def field[W, R](name: String, fromRecord: (Record) => W, conv: Conv[W, R]): F[R] = {
    val v = fromRecord(record)
    valueMap.put(name, conv.to(v))
    null.asInstanceOf[R] // Значение возврата не используется в EsWrite
  }
  override protected def boostField(fromRecord: Record => Double): F[Double] = {
    val v = fromRecord(record)
    val map = valueMap
    map.put(_boostName, v.asInstanceOf[AnyRef])
    map.put(boostName, v.asInstanceOf[AnyRef])
    0.0 // Значение возврата не используется в EsWrite
  }
  override protected def objectListField[CC <: EsRecord, M <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[M#Record], meta: M): FList[CC, M] = {
    val list = fromRecord(record)
    val value = new ju.ArrayList[ju.Map[String, AnyRef]](list.size)
    for (v <- list) {
      val r = v.asInstanceOf[meta.type#Record]
      if (meta.validateRecord(r)) value.add(meta.makeWrite(r).valueMap)
    }
    valueMap.put(name, value)
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

  override protected def idField(fr: (Record) => String): String = data.getId

  override protected def field[W, R](name: String, fromRecord: Record => W, conv: Conv[W, R]): F[R] = {
    try {
      data.getHighlightedField(name) match {
        case null => conv.from(data.get(name))
        case hlt => conv.from(hlt.fragments()(0).string())
      }
    } catch {
      case e: Throwable => throw new RuntimeException(s"Error converting field '$name' from value '${data.get(name)}' in item id:${data.getId}", e)
    }
  }
  override protected def boostField(fromRecord: Record => Double): F[Double] = {
    data.get(boostName).asInstanceOf[Double]
  }
  override protected def objectListField[CC <: EsRecord, A <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[A#Record], adapter: A): FList[CC, A] = {
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
  */
class EsField(val name: String) {
  def boostedName(boost: Int): String = if (boost == 1) name else name + "^" + boost

  def termQuery(value: Any): TermQueryBuilder = new TermQueryBuilder(name, value)
  def inQuery(values: Iterable[_]): TermsQueryBuilder = new TermsQueryBuilder(name, asJavaIterable(values))
  def rangeQuery: RangeQueryBuilder = new RangeQueryBuilder(name)
  def existsQuery: ExistsQueryBuilder = new ExistsQueryBuilder(name)
}

/**
  * Объект, реализующий этот trait, имеет перечень полей, заданных в [[EsTrait]],
  * а также методы для создания и получения объектов [[EsRecord]] и [[EsTypeRecord]].
  */
trait EsMeta[C <: EsRecord] extends EsTrait {
  override type F[T] = EsField
  override type FList[CC <: EsRecord, M <: EsSubTypeMeta[CC]] = M

  /** This field name (without dots), for example: "section" */
  def fieldName: String
  /** Full path to this field with dots, for example: "section.item" */
  def path: String

  def subFieldName(name: String): String = {
    val p: String = path
    if (p.isEmpty) name else p + "." + name
  }

  override protected def idField(fr: (Record) => String): EsField =
    new EsField(subFieldName("_id"))

  override protected def field[W, R](name: String, fromRecord: Record => W, conv: Conv[W, R]): F[R] =
    new EsField(subFieldName(name))

  override protected def boostField(fromRecord: Record => Double): F[Double] =
    new EsField(subFieldName(boostName))

  override protected def objectListField[CC <: EsRecord, M <: EsSubTypeMeta[CC]]
  (name: String, fromRecord: Record => Iterable[M#Record], meta: M): FList[CC, M] =
    meta.withPath(name, subFieldName(name)).asInstanceOf[FList[CC, M]]

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
  override def makeWrite(r: Record): EsTypeWrite
  override def fieldName: String = elasticMapping.tpe
  override def path: String = ""

  def writeFactory: Record => Option[EsTypeWrite] = (r: Record) => if (validateRecord(r)) Some(makeWrite(r)) else None
}

/**
  * [[EsMeta]] для вложенного объекта.
  */
trait EsSubTypeMeta[C <: EsRecord] extends EsMeta[C] {
  def withPath(name: String, path: String): EsSubTypeMeta[C]

  def nestedQuery(query: QueryBuilder, scoreMode: ScoreMode = ScoreMode.Avg): NestedQueryBuilder = {
    require(!path.isEmpty, "nestedQuery can be called only on parent type field, not on subtype object")
    new NestedQueryBuilder(path, query, scoreMode)
  }
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

    new EsResult[C](took = response.getTook.millis(),
      found = hits.getTotalHits.toInt,
      rows = hits.getHits.map(meta.fromHit)(collection.breakOut),
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
  protected def stat(result: EsResult[C]): EsResult[C] = {statAddOne(result.took); result}
  protected def stat(result: SearchResponse): SearchResponse = {statAddOne(result.getTook.millis()); result}

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

  def get(id: String)(block: GetRequestBuilder => Any = a => ()): Option[C] =
    meta.fromGet(statByRealTime(em.get(id)(block)))

  def getById(id: Int): Option[C] = get(id.toString)()
  def getById(id: String): Option[C] = get(id)()

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
    val mlt: MoreLikeThisQueryBuilder = moreLikeThisQuery(Array(new Item(em.index, em.tpe, id)))
    var search: SearchRequestBuilder = null
    val resp = em.search {b =>
      block(mlt)
      b.setQuery(mlt)
      search = b
    }
    stat(searchResponse(search, resp))
  }
}
