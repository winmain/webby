package webby.commons.cache

import java.util.concurrent.TimeUnit
import java.{lang => jl}

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

/**
  * Кеш, основанный на guava [[LoadingCache]].
  * Поэтому, null'ы недопустимы в качестве ключей и значений кеша.
  * Если требуется хранить примитивы (типа Int), тогда следует использовать обёртку Option[Int] для этого.
  */
abstract class TimedCache[K, RealK, V](val cache: LoadingCache[RealK, V]) {
  def get(key: K): V = cache.get(transformKey(key))

  def invalidate(key: K): Unit = cache.invalidate(transformKey(key))
  def refresh(key: K): Unit = cache.refresh(transformKey(key))

  protected def transformKey(key: K): RealK
}

object TimedCache {

  def default[K <: AnyRef, V <: AnyRef](expireDuration: Long, expireUnit: TimeUnit)(cacheLoader: K => V): TimedCache[K, K, V] =
    new TimedCache[K, K, V](
      CacheBuilder.newBuilder().expireAfterWrite(expireDuration, expireUnit).build(new CacheLoader[K, V] {
        def load(key: K): V = cacheLoader(key)
      })) {
      protected def transformKey(key: K): K = key
    }

  def intKey[V <: AnyRef](expireDuration: Long, expireUnit: TimeUnit)(cacheLoader: Int => V): TimedCache[Int, jl.Integer, V] =
    new TimedCache[Int, jl.Integer, V](
      CacheBuilder.newBuilder().expireAfterWrite(expireDuration, expireUnit).build(new CacheLoader[jl.Integer, V] {
        def load(key: jl.Integer): V = cacheLoader(key)
      })) {
      protected def transformKey(key: Int): Integer = key
    }

  def stringKey[V <: AnyRef](expireDuration: Long, expireUnit: TimeUnit)(cacheLoader: String => V): TimedCache[String, String, V] =
    default[String, V](expireDuration, expireUnit)(cacheLoader)
}
