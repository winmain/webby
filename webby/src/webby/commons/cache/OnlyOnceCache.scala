package webby.commons.cache

import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import webby.commons.concurrent.LockSet

/**
  * Класс, служащий в качестве обёртки над каким-либо сервисом. Спасает от множества повторяющихся
  * запросов, т.к., выстраивает их в очередь, и кеширует результаты по ключу LockKey. Т.е., среди всех
  * запросов с одинаковым LockKey выполняется только первый, а остальные получают результаты кеширования.
  *
  * @param expireDuration Время устаревания кеша с момента последней записи.
  * @param expireUnit     Временная единица expireDuration
  * @param lockStripes    the minimum number of stripes (locks) for LockKey (see LockSet)
  * @tparam Req     Запрос (должен поддерживать equals). Желательно делать case class'ом.
  * @tparam Resp    Ответ
  * @tparam LockKey Ключ блокировки (должен поддерживать hashcode, equals). Желательно делать case class'ом, либо примитивом.
  */
case class OnlyOnceCache[Req <: AnyRef, Resp <: AnyRef, LockKey <: AnyRef](expireDuration: Long,
                                                                           expireUnit: TimeUnit,
                                                                           lockStripes: Int = 32,
                                                                           maximumSize: Long = 1000L) {
  private val lockSet = LockSet.lock[LockKey](lockStripes)

  private val cache = CacheBuilder.newBuilder()
    .expireAfterWrite(expireDuration, expireUnit)
    .maximumSize(maximumSize)
    .build[LockKey, (Req, Resp)]()

  def action(req: Req, getKey: Req => LockKey)(body: Req => Resp, onCached: Resp => Resp = a => a): Resp = {
    val lockKey: LockKey = getKey(req)
    lockSet.withLock(lockKey) {
      val cached: (Req, Resp) = cache.getIfPresent(lockKey)
      if (cached != null && cached._1 == req) onCached(cached._2)
      else {
        val result = body(req)
        cache.put(lockKey, req -> result)
        result
      }
    }
  }
}
