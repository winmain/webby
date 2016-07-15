package webby.commons.cache

import java.time.{LocalDateTime, ZoneId}

import com.google.common.net.HttpHeaders
import webby.api.mvc._
import webby.commons.codec.MD5
import webby.commons.date.StdDates

/**
  * Простой кешер, кеширующий только одно значение.
  * При устаревании кеша внутреннее значение обновляет только один поток, пока остальные получают старые данные.
  * Это позволяет избежать блокировки во время обновления кеша. Но получение первого значения блокирует все потоки, пока это значение не запишется в кешер.
  * cacheLoader не может возвращать null.
  */
class SimpleCache[V](expireMillis: Long, cacheLoader: => V) {
  private var value: V = null.asInstanceOf[V]
  private var expireOn: Long = 0
  @volatile private var updatingNow: Boolean = false
  private val updateSync = new Object

  def get: V = {
    val now: Long = System.currentTimeMillis()
    if (now >= expireOn) getExpired else value
  }

  /**
    * Проверить, поменялась ли запись с момента заданного времени?
    * Если да, то вернуть Some(value). Если же не поменялась, то None.
    */
  def getIfModifiedSince(time: Long): Option[V] = {
    if (time >= expireOn) Some(getExpired) else None
  }

  // ------------------------------- Private & protected methods -------------------------------

  protected def getExpired: V = {
    val needUpdate: Boolean =
      synchronized {
        if (!updatingNow) {updatingNow = true; true} else false
      }
    if (needUpdate) {
      updateSync.synchronized {
        try {
          value = cacheLoader
        } catch {
          case e: Exception =>
            updatingNow = false
            throw e
        }
        require(value != null, "CacheLoader cannot return null")
        expireOn = System.currentTimeMillis() + expireMillis
        updatingNow = false
        value
      }
    } else {
      while (value == null) updateSync.synchronized(())
      value
    }
  }
}

/**
  * Вариант [[SimpleCache]], который умеет обрабатывать запросы с заголовками if-modified-since,
  * if-none-match, умеет выставлять e-tag.
  */
class SimpleCachePlainResultWithETag(expireMillis: Long, cacheLoader: => PlainResult)
  extends SimpleCache[(PlainResult, String)](expireMillis, {
    val result: PlainResult = cacheLoader
    val eTag: String = MD5.hex(result.body)
    result.withHeader(HttpHeaders.ETAG, eTag) -> eTag
  }) {

  def eTagFrom(result: PlainResult): String = MD5.hex(result.body)

  def handle(req: RequestHeader): PlainResult = {
    // Сначала проверяем, работает ли клиент по E-TAG'у
    req.headers.get(HttpHeaders.IF_NONE_MATCH) match {
      case Some(ifNoneMatch) =>
        val (result, eTag) = get
        if (ifNoneMatch == eTag) Results.NotModified
        else result

      case None =>
        // Потом, проверяем, поддерживает ли клиент if-modified-since
        req.headers.get(HttpHeaders.IF_MODIFIED_SINCE) match {
          case Some(ifModifiedStr) =>
            val ifModified: Long =
              try StdDates.toMillis(LocalDateTime.parse(ifModifiedStr, StdDates.httpDateFormat), ZoneId.systemDefault())
              catch {case e: Exception => throw ResultException(get._1)}
            getIfModifiedSince(ifModified) match {
              case Some(v) => v._1
              case None => Results.NotModified
            }

          case None => get._1
        }
    }
  }

  def action = SimpleAction {implicit req =>
    handle(req)
  }
}
