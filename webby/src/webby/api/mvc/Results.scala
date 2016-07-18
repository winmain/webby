package webby.api.mvc

import java.nio.file.{Files, Path}

import com.google.common.base.Charsets
import com.google.common.net.HttpHeaders
import com.google.common.net.HttpHeaders._
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpResponseStatus._
import webby.api.http.ContentTypes
import webby.commons.io.{StdJs, Url, UrlEncoder}
import webby.commons.text.html.StdHtmlView

/**
  * Generates default `SimpleResult` from a content type, headers and content.
  *
  * @param status the HTTP response status, e.g ‘200 OK’
  */
class ResultStatus(status: HttpResponseStatus) extends PlainResult(status, body = Array.emptyByteArray) {

  def html(content: String): PlainResult = apply(content, ContentTypes.textHtmlUtf8)
  def text(content: String): PlainResult = apply(content, ContentTypes.textPlainUtf8)

  def apply(bytes: Array[Byte]): PlainResult = {
    body = bytes
    this
  }

  def apply(plainText: String): PlainResult = apply(plainText.getBytes(Charsets.UTF_8))

  def apply(bytes: Array[Byte], contentType: String): PlainResult = {
    body = bytes
    withHeader(CONTENT_TYPE, contentType)
  }

  def apply(content: String, contentType: String): PlainResult = apply(content.getBytes(Charsets.UTF_8), contentType)
}

class ResultOk extends ResultStatus(OK) {
  /**
    * Send a file.
    *
    * @param path     The file to send
    * @param inline   Use Content-Disposition inline or attachment.
    * @param fileName function to retrieve the file name (only used for Content-Disposition attachment)
    */
  def sendFile(path: Path, inline: Boolean = false, fileName: Path => String = _.getFileName.toString): PlainResult = {
    body = Files.readAllBytes(path)
    withHeader(CONTENT_TYPE, webby.api.libs.MimeTypes.forFileName(path.getFileName.toString).getOrElse(webby.api.http.ContentTypes.BINARY))
    if (!inline) withHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + fileName(path) + "\"")
    this
  }
}


/** Helper utilities to generate results. */
trait Results {
  //  import webby.api.http.Status._
  import io.netty.handler.codec.http.HttpResponseStatus._

  // ---------------------- Return codes ----------------------

  /** Generates a ‘200 OK’ result. */
  def Ok: ResultOk = new ResultOk
  def Ok(htmlView: StdHtmlView): PlainResult = OkHtml(htmlView.result)
  def OkHtml(htmlViewResult: String): PlainResult = Ok(htmlViewResult, ContentTypes.textHtmlUtf8)
  def JsonOk(obj: Any): PlainResult = StdJs.get.result(obj)

  /** Generates a ‘201 CREATED’ result. */
  def Created = new ResultStatus(CREATED)

  /** Generates a ‘202 ACCEPTED’ result. */
  def Accepted = new ResultStatus(ACCEPTED)

  /** Generates a ‘203 NON_AUTHORITATIVE_INFORMATION’ result. */
  def NonAuthoritativeInformation = new ResultStatus(NON_AUTHORITATIVE_INFORMATION)

  /** Generates a ‘204 NO_CONTENT’ result. */
  def NoContent = PlainResult(NO_CONTENT)

  /** Generates a ‘205 RESET_CONTENT’ result. */
  def ResetContent = PlainResult(RESET_CONTENT)

  /** Generates a ‘206 PARTIAL_CONTENT’ result. */
  def PartialContent = new ResultStatus(PARTIAL_CONTENT)

  /** Generates a ‘207 MULTI_STATUS’ result. */
  def MultiStatus = new ResultStatus(MULTI_STATUS)

  /**
    * Generates a ‘301 MOVED_PERMANENTLY’ simple result.
    *
    * @param url the URL to redirect to
    */
  def MovedPermanently(url: String): PlainResult = Redirect(url, MOVED_PERMANENTLY)

  def MovedPermanently(url: Url): PlainResult = MovedPermanently(url.url)


  /**
    * Generates a ‘302 FOUND’ simple result.
    *
    * @param url the URL to redirect to
    */
  def Found(url: String): PlainResult = Redirect(url, FOUND)

  /**
    * Generates a ‘303 SEE_OTHER’ simple result.
    *
    * @param url the URL to redirect to
    */
  def SeeOther(url: String): PlainResult = Redirect(url, SEE_OTHER)

  /** Generates a ‘304 NOT_MODIFIED’ result. */
  def NotModified = PlainResult(NOT_MODIFIED)

  /**
    * Generates a ‘307 TEMPORARY_REDIRECT’ simple result.
    *
    * @param url the URL to redirect to
    */
  def TemporaryRedirect(url: String): PlainResult = Redirect(url, TEMPORARY_REDIRECT)

  /** Generates a ‘400 BAD_REQUEST’ result. */
  def BadRequest = new ResultStatus(BAD_REQUEST)

  /** Generates a ‘401 UNAUTHORIZED’ result. */
  def Unauthorized = new ResultStatus(UNAUTHORIZED)

  /** Generates a ‘403 FORBIDDEN’ result. */
  def Forbidden = new ResultStatus(FORBIDDEN)

  /** Generates a ‘404 NOT_FOUND’ result. */
  def NotFoundRaw = new ResultStatus(NOT_FOUND)

  /** Generates a ‘405 METHOD_NOT_ALLOWED’ result. */
  def MethodNotAllowed = new ResultStatus(METHOD_NOT_ALLOWED)

  /** Generates a ‘406 NOT_ACCEPTABLE’ result. */
  def NotAcceptable = new ResultStatus(NOT_ACCEPTABLE)

  /** Generates a ‘408 REQUEST_TIMEOUT’ result. */
  def RequestTimeout = new ResultStatus(REQUEST_TIMEOUT)

  /** Generates a ‘409 CONFLICT’ result. */
  def Conflict = new ResultStatus(CONFLICT)

  /** Generates a ‘410 GONE’ result. */
  def Gone = new ResultStatus(GONE)

  /** Generates a ‘412 PRECONDITION_FAILED’ result. */
  def PreconditionFailed = new ResultStatus(PRECONDITION_FAILED)

  /** Generates a ‘413 REQUEST_ENTITY_TOO_LARGE’ result. */
  def EntityTooLarge = new ResultStatus(REQUEST_ENTITY_TOO_LARGE)

  /** Generates a ‘414 REQUEST_URI_TOO_LONG’ result. */
  def UriTooLong = new ResultStatus(REQUEST_URI_TOO_LONG)

  /** Generates a ‘415 UNSUPPORTED_MEDIA_TYPE’ result. */
  def UnsupportedMediaType = new ResultStatus(UNSUPPORTED_MEDIA_TYPE)

  /** Generates a ‘417 EXPECTATION_FAILED’ result. */
  def ExpectationFailed = new ResultStatus(EXPECTATION_FAILED)

  /** Generates a ‘422 UNPROCESSABLE_ENTITY’ result. */
  def UnprocessableEntity = new ResultStatus(UNPROCESSABLE_ENTITY)

  /** Generates a ‘423 LOCKED’ result. */
  def Locked = new ResultStatus(LOCKED)

  /** Generates a ‘424 FAILED_DEPENDENCY’ result. */
  def FailedDependency = new ResultStatus(FAILED_DEPENDENCY)

  /** Generates a ‘429 TOO_MANY_REQUESTS’ result. */
  def TooManyRequests = new ResultStatus(TOO_MANY_REQUESTS)

  /** Generates a ‘500 INTERNAL_SERVER_ERROR’ result. */
  def InternalServerError = new ResultStatus(INTERNAL_SERVER_ERROR)

  /** Generates a ‘501 NOT_IMPLEMENTED’ result. */
  def NotImplemented = new ResultStatus(NOT_IMPLEMENTED)

  /** Generates a ‘502 BAD_GATEWAY’ result. */
  def BadGateway = new ResultStatus(BAD_GATEWAY)

  /** Generates a ‘503 SERVICE_UNAVAILABLE’ result. */
  def ServiceUnavailable = new ResultStatus(SERVICE_UNAVAILABLE)

  /** Generates a ‘504 GATEWAY_TIMEOUT’ result. */
  def GatewayTimeout = new ResultStatus(GATEWAY_TIMEOUT)

  /** Generates a ‘505 HTTP_VERSION_NOT_SUPPORTED’ result. */
  def HttpVersionNotSupported = new ResultStatus(HTTP_VERSION_NOT_SUPPORTED)

  /** Generates a ‘507 INSUFFICIENT_STORAGE’ result. */
  def InsufficientStorage = new ResultStatus(INSUFFICIENT_STORAGE)

  // ------------------------------- Additional helper methods -------------------------------

  /**
    * Generates a simple result.
    *
    * @param status the status code
    */
  def Status(status: HttpResponseStatus): ResultStatus = new ResultStatus(status)

  /**
    * Generates a redirect simple result.
    *
    * @param url    the URL to redirect to
    * @param status HTTP status
    */
  def Redirect(url: String, status: HttpResponseStatus = SEE_OTHER): PlainResult = {
    new ResultStatus(status).withHeader(LOCATION, url)
  }

  def Redirect(url: Url): PlainResult = Redirect(url.url)

  /** Редирект с урлом, который содержит кириллицу */
  def RedirectCyrillic(url: String): PlainResult = Redirect(UrlEncoder.encodeCyrillic(url))

  def FileResult(bytes: Array[Byte], name: String): PlainResult = Ok(bytes).withHeader(
    HttpHeaders.CONTENT_DISPOSITION, "attachment; filename = \"" + name + "\"")

  // ------------------------------- throw ResultExceptions -------------------------------

  def throwResult(result: Result): Nothing = throw ResultException(result)

  def throwOk(message: String): Nothing = throw ResultException(Ok(message))
  def throwOk(html: StdHtmlView): Nothing = throw ResultException(Ok(html))
  def throwOkUtf8(message: String): Nothing = throw ResultException(Ok(message).withHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8"))
  def throwBadRequest(message: String): Nothing = throw ResultException(BadRequest(message))

  def throwRedirect(url: Url): Nothing = throw ResultException(Redirect(url))
  def throwRedirectCyrillic(url: String): Nothing = throw ResultException(RedirectCyrillic(url))

  def throwNotFoundRaw: Nothing = throw ResultException(NotFoundRaw)
  def throwNotFoundRaw(message: String): Nothing = throw ResultException(NotFoundRaw(message))
}

/** Helper utilities to generate results. */
object Results extends Results
