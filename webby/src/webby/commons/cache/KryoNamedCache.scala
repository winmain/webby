package webby.commons.cache

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import webby.api.App

import scala.reflect.ClassTag

/**
  * Версия [[NamedCache]], которая использует библиотеку [[Kryo]] для сериализации данных в кеше.
  *
  * Requires sbt dependencies
  * {{{
  *   // TODO:
  * }}}
  *
  * @param cacheName   Имя кеша в хранилище ehcache
  * @param beanVersion Версия Bean класса [[B]]. При изменении структуры класса [[B]], его версию
  *                    следует увеличить, чтобы избежать ошибок десериализации классов старой версии.
  *                    Хранилище устроено так, что оно может содержать объекты только одной версии.
  *                    Если версия [[beanVersion]] отличается от версии в хранилище, то хранилище
  *                    полностью очищается при инициализации.
  * @param init        Дополнительная процедура инициализации [[Kryo]]
  * @tparam B Bean класс, который будет храниться в этом хранилище, и сериализоваться через [[Kryo]].
  */
class KryoNamedCache[B: ClassTag](cacheName: String,
                                  beanVersion: Int,
                                  init: Kryo => Unit = _ => ()) {
  val cache: NamedCache = new NamedCache(cacheName)
  private val versionKey = "_version"
  private val beanClass = implicitly[ClassTag[B]].runtimeClass.asInstanceOf[Class[B]]
  private val kryo = new ThreadLocal[Kryo] {
    override def initialValue(): Kryo = {
      val k = new Kryo
      k.setClassLoader(App.app.classloader)
      k.register(beanClass)
      init(k)
      k
    }
  }

  // Проверить версию бинов в кеше. Сбросить кеш, если версии отличаются.
  if (!cache.get[Int](versionKey).contains(beanVersion)) cache.removeAll()
  cache.setEternal(versionKey, beanVersion)

  /**
    * Установить значение со стандартным временем жизни (время жизни прописано в ehcache.xml в поле timeToLiveSeconds).
    */
  def set(key: String, bean: B) = cache.set(key, toValue(bean))

  /**
    * Установить значение и время его жизни (поле timeToLiveSeconds в ehcache.xml игнорируется).
    * @param timeToLiveSeconds Время жизни в секундах, либо 0 если значение должно храниться вечно.
    */
  def set(key: String, bean: B, timeToLiveSeconds: Int) = cache.set(key, toValue(bean), timeToLiveSeconds)

  /**
    * Установить вечное значение (поле timeToLiveSeconds в ehcache.xml игнорируется)
    */
  def setEternal(key: String, bean: B) = cache.set(key, toValue(bean))

  def get(key: String): Option[B] = {
    val element = cache.cache.get(key)
    if (element == null) None
    else Some(fromValue(element.getObjectValue.asInstanceOf[Array[Byte]]))
  }

  def get(key: String, default: => B): B = {
    val element = cache.cache.get(key)
    if (element == null) default
    else fromValue(element.getObjectValue.asInstanceOf[Array[Byte]])
  }

  def contains(key: String): Boolean = cache.contains(key)

  /**
    * Удалить объект из кеша.
    * @return true if the element was removed, false if it was not found in the cache
    */
  def remove(key: String): Boolean = cache.remove(key)

  /**
    * Полностью очистить кеш
    */
  def removeAll(): Unit = cache.removeAll()

  /**
    * Выполнить действие для каждого элемента кэша
    * Подразумевается, что вы знаете что делаете, потому что кэш может быть большим
    *
    * @param callback функция, которая принимает на вход ключ кэша и его значение
    */
  def foreach(callback: (String, B) => Unit) = {
    cache.getKeys.foreach {key =>
      if (!versionKey.equals(key)) get(key).foreach {callback(key, _)}
    }
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def toValue(bean: B): Array[Byte] = {
    val output: Output = new Output(256, 1024 * 1024)
    kryo.get().writeObject(output, bean)
    output.close()
    output.getBuffer
  }

  private def fromValue(bytes: Array[Byte]): B = {
    val input: Input = new Input(bytes)
    val bean: B = kryo.get().readObject[B](input, beanClass)
    input.close()
    bean
  }
}
