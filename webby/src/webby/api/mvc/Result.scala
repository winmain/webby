package webby.api.mvc
import java.time.LocalDateTime

import com.google.common.net.HttpHeaders._
import io.netty.handler.codec.http
import io.netty.handler.codec.http.HttpResponseStatus
import webby.commons.system.log.PageLog
import webby.commons.time.StdDates

import scala.collection.mutable
import scala.concurrent._

/**
  * Able to return [[Result]].
  */
trait Resultable {
  def toResult: Result
}

/**
  * Any Action result.
  */
sealed trait Result extends WithHeaders[Result] with Resultable {
  override def toResult: Result = this
}

sealed trait WithHeaders[+A <: Result] {
  /**
    * Adds HTTP headers to this result.
    */
  def withHeader(name: String, value: String): A

  def withLastModified(ltd: LocalDateTime): A = withHeader(LAST_MODIFIED, StdDates.httpDateFormatLDT(ltd))
  def withNoCache: A = withHeader(CACHE_CONTROL, "no-cache")

  /** Чтобы можно было делать кроссоменные запросы к этому урлу */
  def withAccessControlAllowOrigin: A = withHeader("Access-Control-Allow-Origin", "*")

  /**
    * Adds cookies to this result.
    *
    * For example:
    * {{{
    * Ok("Hello world").withCookies(Cookie("theme", "blue"))
    * }}}
    *
    * @param cookie the cookie to add to this result
    * @return the new result
    */
  def withCookie(cookie: Cookie): A

  def withToast(text: String): A = withCookie(Cookie("toast", text, httpOnly = false))
  def withToast(title: String, text: String): A = withToast("<header>" + title + "</header>" + text)

  /**
    * Changes the result content type.
    *
    * For example:
    * {{{
    * Ok("<text>Hello world</text>").as("text/xml")
    * }}}
    *
    * @param contentType the new content type.
    * @return the new result
    */
  def as(contentType: String): A

  def asPlainText: A = withHeader(CONTENT_TYPE, "text/plain; charset=utf-8")
  def asXml: A = withHeader(CONTENT_TYPE, "application/xml; charset=utf-8")
  def asPdf(filename: String) = as("application/pdf").withHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
  def asRtf(filename: String) = as("application/rtf").withHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
}

/**
  * A plain HTTP result.
  */
class PlainResult(var status: HttpResponseStatus, var body: Array[Byte]) extends Result with WithHeaders[PlainResult] {
  val headers: mutable.Buffer[(String, String)] = mutable.Buffer[(String, String)]()
  val cookies: mutable.Buffer[http.cookie.Cookie] = mutable.Buffer[http.cookie.Cookie]()

  /**
    * Adds HTTP headers to this result.
    */
  override def withHeader(name: String, value: String): this.type = {
    headers += ((name, value))
    this
  }

  /**
    * Adds cookies to this result.
    *
    * For example:
    * {{{
    * Ok("Hello world").withCookies(Cookie("theme", "blue"))
    * }}}
    *
    * @return the new result
    */
  override def withCookie(cookie: Cookie): this.type = {
    cookies += cookie
    this
  }

  /**
    * Changes the result content type.
    *
    * For example:
    * {{{
    * Ok("<text>Hello world</text>").as("text/xml")
    * }}}
    *
    * @param contentType the new content type.
    * @return the new result
    */
  override def as(contentType: String): PlainResult = withHeader(CONTENT_TYPE, contentType)
}

object PlainResult {
  def apply(status: HttpResponseStatus, body: Array[Byte] = Array.emptyByteArray): PlainResult = new PlainResult(status, body)
}


/**
  * An `AsyncResult` handles a `Promise` of result for cases where the result is not ready yet.
  *
  * @param body the promise of result, which can be any other result type
  */
class AsyncResult(rh: RequestHeader, body: => Result)(implicit val ec: ExecutionContext) extends Result with WithHeaders[AsyncResult] {
  private var finalizers: List[Result => Result] = Nil
  private var executed = false

  def execute(pageLog: PageLog): Future[Result] = {
    if (executed) throw new IllegalStateException("AsyncResult already executed")
    executed = true
    Future {
      PageLog.set(pageLog)
      try {
        ActionTools.executeWithFinalizers(body, rh, finalizers)
      } catch {
        case e: ResultException => e.getResult(rh)
      }
      finally PageLog.remove()
    }
  }

  def prependFinalizer(finalizer: Result => Result): Unit = {
    require(!executed, "Cannot prepend finalizer to executed AsyncResult")
    finalizers ::= finalizer
  }

  /**
    * Adds headers to this result.
    *
    * For example:
    * {{{
    * Ok("Hello world").withHeaders(ETAG -> "0")
    * }}}
    *
    * @return the new result
    */
  def withHeader(name: String, value: String): AsyncResult = {prependFinalizer(_.withHeader(name, value)); this}

  /**
    * Adds cookies to this result.
    *
    * For example:
    * {{{
    * Ok("Hello world").withCookies(Cookie("theme", "blue"))
    * }}}
    *
    * @return the new result
    */
  def withCookie(cookie: Cookie): AsyncResult = {prependFinalizer(_.withCookie(cookie)); this}

  /**
    * Changes the result content type.
    *
    * For example:
    * {{{
    * Ok("<text>Hello world</text>").as("text/xml")
    * }}}
    *
    * @param contentType the new content type.
    * @return the new result
    */
  def as(contentType: String): AsyncResult = {prependFinalizer(_.as(contentType)); this}
}
