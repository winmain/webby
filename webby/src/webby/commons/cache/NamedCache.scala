package webby.commons.cache

import java.util

import net.sf.ehcache.{Cache, CacheManager, Element}
import webby.api.App

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Кеш, использующий ehcache.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "net.sf.ehcache" % "ehcache-core" % "2.6.11"
  * }}}
  *
  * @param name Название кеша, которое используется в конфиге ehcache.xml
  */
class NamedCache(val name: String) {
  private[cache] val cache: Cache = {
    val manager: CacheManager =
      App.app.plugin[CachePlugin].getOrElse(sys.error("CachePlugin is disabled")).manager
    manager.addCacheIfAbsent(name)
    manager.getCache(name)
  }

  /**
    * Установить значение со стандартным временем жизни (время жизни прописано в ehcache.xml в поле timeToLiveSeconds).
    */
  def set(key: String, value: Any) {
    cache.put(new Element(key, value))
  }

  /**
    * Установить значение и время его жизни (поле timeToLiveSeconds в ehcache.xml игнорируется).
    * @param timeToLiveSeconds Время жизни в секундах, либо 0 если значение должно храниться вечно.
    */
  def set(key: String, value: Any, timeToLiveSeconds: Int) {
    val element = new Element(key, value)
    if (timeToLiveSeconds == 0) element.setEternal(true)
    element.setTimeToLive(timeToLiveSeconds)
    cache.put(element)
  }

  /**
    * Установить вечное значение (поле timeToLiveSeconds в ehcache.xml игнорируется)
    */
  def setEternal(key: String, value: Any) = set(key, value, 0)

  def getAny(key: String): Option[Any] = {
    val element = cache.get(key)
    if (element == null) None
    else Some(element.getObjectValue)
  }

  def get[T](key: String): Option[T] = {
    val element = cache.get(key)
    if (element == null) None
    else Some(element.getObjectValue.asInstanceOf[T])
  }

  def get[T](key: String, default: T): T = {
    val element = cache.get(key)
    if (element == null) default
    else element.getObjectValue.asInstanceOf[T]
  }

  /**
    * Retrieve a value from the cache for the given type
    *
    * @param key Item key.
    * @return result as Option[T]
    */
  def getAs[T](key: String)(implicit m: ClassTag[T]): Option[T] = {
    getAny(key).map {item =>
      if (m.runtimeClass.isAssignableFrom(item.getClass)) Some(item.asInstanceOf[T]) else None
    }.getOrElse(None)
  }

  /**
    * Gets all the elements from the cache for the keys provided. Updates Element Statistics.
    * Returned Map may contain less or more keys if collection is modified before call completes.
    * Throws a NullPointerException if any key in the collection is null
    * <p/>
    * Note that the Element's lastAccessTime is always the time of this get.
    */
  def getAll(keys: Iterable[String]): mutable.Map[String, AnyRef] = cache.getAll(keys.asJavaCollection).asScala.map {e =>
    e._1.toString -> (e._2 match {
      case null => null
      case v => v.getObjectValue
    })
  }

  def getKeysRaw: util.List[_] = cache.getKeys
  def getKeys: Iterator[String] = getKeysRaw.asScala.toIterator.map(_.asInstanceOf[String])
  def getKeysNoDuplicateCheckRaw: util.List[_] = cache.getKeysNoDuplicateCheck
  def getKeysNoDuplicateCheck: Iterator[String] = getKeysNoDuplicateCheckRaw.asScala.toIterator.map(_.asInstanceOf[String])
  def getKeysWithExpiryCheckRaw: util.List[_] = cache.getKeysWithExpiryCheck
  def getKeysWithExpiryCheck: Iterator[String] = getKeysWithExpiryCheckRaw.asScala.toIterator.map(_.asInstanceOf[String])

  def contains(key: String): Boolean = {
    // cache.isKeyInCache не используется, потому что он не всегда возвращает актуальный результат
    cache.getQuiet(key) != null
  }

  /**
    * Увеличить счётчик типа Int на величину value. Возвращает новое значение счётчика после увеличения.
    * Также, устанавливается время жизни счётчика (поле timeToLiveSeconds в ehcache.xml игнорируется).
    */
  def inc(key: String, value: Int, timeToLiveSeconds: Int): Int = {
    val newVal = get[Int](key, 0) + value
    set(key, newVal, timeToLiveSeconds)
    newVal
  }

  /**
    * Увеличить счётчик типа Int на величину value. Возвращает новое значение счётчика после увеличения.
    * Время жизни счётчика берём из поля timeToLiveSeconds в ehcache.xml
    */
  def inc(key: String, value: Int): Int = {
    val newVal = get[Int](key, 0) + value
    set(key, newVal)
    newVal
  }

  /**
    * Удалить объект из кеша.
    * @return true if the element was removed, false if it was not found in the cache
    */
  def remove(key: String): Boolean = cache.remove(key)

  /**
    * Полностью очистить кеш
    */
  def removeAll(): Unit = cache.removeAll()
}
