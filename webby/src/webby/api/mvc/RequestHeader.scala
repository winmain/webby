package webby.api.mvc
import com.google.common.net.HttpHeaders
import io.netty.handler.codec.http.HttpMethod
import webby.api.http.MediaRange

import scala.annotation.implicitNotFound

/**
  * The HTTP request header. Note that it doesn’t contain the request body yet.
  */
@implicitNotFound("Cannot find any HTTP Request Header here")
trait RequestHeader {

  /**
    * The request ID.
    */
  def requestId: Long

  /**
    * The complete request URI, containing both path and query string.
    */
  def uri: String

  /**
    * The URI path.
    */
  def path: String

  /**
    * The HTTP method.
    */
  def method: HttpMethod

  /**
    * The HTTP version.
    */
  def version: String

  /**
    * The parsed query string.
    */
  def query: UrlEncoded

  /**
    * The HTTP headers.
    */
  def headers: Headers

  /**
    * The client IP address.
    *
    * If the `X-Forwarded-For` header is present, then this method will return the value in that header
    * if either the local address is 127.0.0.1, or if `trustxforwarded` is configured to be true in the
    * application configuration file.
    */
  def remoteAddress: String

  // -- Computed

  /**
    * The HTTP host (domain, optionally port)
    */
  def host: String

  /**
    * The HTTP domain
    */
  def domain: String

  /**
    * The HTTP cookies.
    */
  def cookies: Cookies

  def contentType: ContentTypePair

  /**
    * @return The media types list of the request’s Accept header, sorted by preference (preferred first).
    */
  def acceptedTypes: Seq[webby.api.http.MediaRange]

  /**
    * Check if this request accepts a given media type.
    *
    * @return true if `mimeType` matches the Accept header, otherwise false
    */
  def accepts(mimeType: String): Boolean

  def isGet: Boolean = method eq HttpMethod.GET
  def isPost: Boolean = method eq HttpMethod.POST
}

/**
  * Базовые реализации методов RequestHeader
  */
abstract class BaseRequestHeader extends RequestHeader {
  override val host: String = headers.get(HttpHeaders.HOST).getOrElse("")

  override val domain: String = host.indexOf(':') match {
    case -1 => host
    case idx => host.substring(0, idx)
  }
  override lazy val cookies: Cookies = Cookies.fromHeader(headers.get(HttpHeaders.COOKIE))

  override lazy val contentType: ContentTypePair = ContentTypePair.parse(headers.http.get(HttpHeaders.CONTENT_TYPE))

  override lazy val acceptedTypes: Seq[webby.api.http.MediaRange] = {
    val mediaTypes = acceptHeader(HttpHeaders.ACCEPT).map(item => (item._1, MediaRange(item._2)))
    mediaTypes.sorted.map(_._2).reverse
  }

  /**
    * @return The items of an Accept* header, with their q-value.
    */
  private def acceptHeader(headerName: String): Seq[(Double, String)] = {
    for {
      header <- headers.get(headerName).toSeq
      value0 <- header.split(',')
      value = value0.trim
    } yield {
      RequestHeader.qPattern.findFirstMatchIn(value) match {
        case Some(m) => (m.group(1).toDouble, m.before.toString)
        case None => (1.0, value) // “The default value is q=1.”
      }
    }
  }

  /**
    * Check if this request accepts a given media type.
    *
    * @return true if `mimeType` matches the Accept header, otherwise false
    */
  override def accepts(mimeType: String): Boolean = {
    acceptedTypes.isEmpty || acceptedTypes.exists(_.accepts(mimeType))
  }

  override def toString: String = method.name() + " " + uri
}

abstract class WrappedRequestHeader(rh: RequestHeader) extends RequestHeader {
  override def requestId: Long = rh.requestId
  override def uri: String = rh.uri
  override def path: String = rh.path
  override def method: HttpMethod = rh.method
  override def version: String = rh.version
  override def query: UrlEncoded = rh.query
  override def headers: Headers = rh.headers
  override def remoteAddress: String = rh.remoteAddress
  override def host: String = rh.host
  override def domain: String = rh.domain
  override def cookies: Cookies = rh.cookies
  override def contentType: ContentTypePair = rh.contentType
  override def acceptedTypes: Seq[MediaRange] = rh.acceptedTypes
  override def accepts(mimeType: String): Boolean = rh.accepts(mimeType)
}

//  override def id: Long = rh.id
//  override def accepts(mimeType: String): Boolean = rh.accepts(mimeType)
//  override def domain: String = rh.domain
//  override def uri: String = rh.uri
//  override def remoteAddress: String = rh.remoteAddress
//  /**
//   * The HTTP method.
//   */
//  override def method: String = ???
//  /**
//   * The HTTP host (domain, optionally port)
//   */
//  override def host: String = ???
//  /**
//   * The HTTP cookies.
//   */
//  override def cookies: Cookies = ???
//  override def contentType: ContentTypePair = ???
//  /**
//   * The HTTP headers.
//   */
//  override def headers: Headers = ???
//  /**
//   * The URI path.
//   */
//  override def path: String = ???
//  /**
//   * @return The media types list of the request’s Accept header, sorted by preference (preferred first).
//   */
//  override def acceptedTypes: Seq[MediaRange] = ???
//  /**
//   * The parsed query string.
//   */
//  override def query: UrlEncoded = ???
//  /**
//   * The HTTP version.
//   */
//  override def version: String = ???
//}


object RequestHeader {
  // “The first "q" parameter (if any) separates the media-range parameter(s) from the accept-params.”
  val qPattern = ";\\s*q=([0-9.]+)".r
}


/**
  * Структура, образующаяся при разборе HTTP заголовка Content-Type
  */
class ContentTypePair(val name: String, val charset: String)

object ContentTypePair {
  val defaultCharset = "utf-8"
  val empty = new ContentTypePair("", defaultCharset)

  /**
    * Разобрать заголовок Content-Type
    */
  def parse(contentTypeHeader: String): ContentTypePair = {
    if (contentTypeHeader == null) empty
    else {
      contentTypeHeader.indexOf(';') match {
        case -1 => new ContentTypePair(contentTypeHeader.trim.toLowerCase, defaultCharset)
        case delimIdx =>
          val name = contentTypeHeader.substring(0, delimIdx).trim.toLowerCase
          contentTypeHeader.indexOf("charset=", delimIdx + 1) match {
            case -1 => new ContentTypePair(name, defaultCharset)
            case charsetIdx => new ContentTypePair(name, contentTypeHeader.substring(charsetIdx + 8).trim)
          }
      }
    }
  }
}

/**
  * The complete HTTP request.
  *
  * @tparam A the body content type.
  */
@implicitNotFound("Cannot find any HTTP Request here")
abstract class Request[+A] extends BaseRequestHeader {self =>

  /**
    * The body content.
    */
  def body: A

  /**
    * Transform the request body.
    */
  def map[B](f: A => B): Request[B] = new Request[B] {
    def requestId = self.requestId
    def uri = self.uri
    def path = self.path
    def method = self.method
    def version = self.version
    def query = self.query
    def headers = self.headers
    def remoteAddress = self.remoteAddress
    lazy val body = f(self.body)
  }
}

object Request {
  def apply[A](rh: RequestHeader, a: A) = new Request[A] {
    def requestId = rh.requestId
    def uri = rh.uri
    def path = rh.path
    def method = rh.method
    def version = rh.version
    def query = rh.query
    def headers = rh.headers
    lazy val remoteAddress = rh.remoteAddress
    def username = None
    val body = a
  }
}

/**
  * Wrap an existing request. Useful to extend a request.
  */
class WrappedRequest[A](request: Request[A]) extends Request[A] {
  def requestId = request.requestId
  def body = request.body
  def headers = request.headers
  def query = request.query
  def path = request.path
  def uri = request.uri
  def method = request.method
  def version = request.version
  def remoteAddress = request.remoteAddress
}
