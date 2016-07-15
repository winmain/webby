package webby.api.http
import webby.api.libs.MimeTypes

/**
 * Defines common HTTP Content-Type header values, according to the current available Codec.
 */
object ContentTypes extends ContentTypes

/** Defines common HTTP Content-Type header values, according to the current available Codec. */
trait ContentTypes {

  /**
   * Content-Type of text.
   */
  def TEXT = withUtf8(MimeTypes.TEXT)

  /**
   * Content-Type of html.
   */
  def HTML = withUtf8(MimeTypes.HTML)

  /**
   * Content-Type of json.
   */
  def JSON = withUtf8(MimeTypes.JSON)

  /**
   * Content-Type of xml.
   */
  def XML = withUtf8(MimeTypes.XML)

  /**
   * Content-Type of css.
   */
  def CSS = withUtf8(MimeTypes.CSS)

  /**
   * Content-Type of javascript.
   */
  def JAVASCRIPT = withUtf8(MimeTypes.JAVASCRIPT)

  /**
   * Content-Type of form-urlencoded.
   */
  def FORM = withUtf8(MimeTypes.FORM)

  /**
   * Content-Type of server sent events.
   */
  def EVENT_STREAM = withUtf8(MimeTypes.EVENT_STREAM)

  /**
   * Content-Type of binary data.
   */
  val BINARY = MimeTypes.BINARY

  /**
   * @return the `codec` charset appended to `mimeType`
   */
  def withUtf8(mimeType: String): String = mimeType + "; charset=utf-8"

  def textHtmlUtf8 = "text/html; charset=utf-8"
  def textPlainUtf8 = "text/plain; charset=utf-8"
}

/**
 * Defines all standard HTTP Status.
 */
object Status extends Status

/**
 * Defines all standard HTTP status codes.
 */
trait Status {

  val CONTINUE = 100
  val SWITCHING_PROTOCOLS = 101

  val OK = 200
  val CREATED = 201
  val ACCEPTED = 202
  val NON_AUTHORITATIVE_INFORMATION = 203
  val NO_CONTENT = 204
  val RESET_CONTENT = 205
  val PARTIAL_CONTENT = 206
  val MULTI_STATUS = 207

  val MULTIPLE_CHOICES = 300
  val MOVED_PERMANENTLY = 301
  val FOUND = 302
  val SEE_OTHER = 303
  val NOT_MODIFIED = 304
  val USE_PROXY = 305
  val TEMPORARY_REDIRECT = 307

  val BAD_REQUEST = 400
  val UNAUTHORIZED = 401
  val PAYMENT_REQUIRED = 402
  val FORBIDDEN = 403
  val NOT_FOUND = 404
  val METHOD_NOT_ALLOWED = 405
  val NOT_ACCEPTABLE = 406
  val PROXY_AUTHENTICATION_REQUIRED = 407
  val REQUEST_TIMEOUT = 408
  val CONFLICT = 409
  val GONE = 410
  val LENGTH_REQUIRED = 411
  val PRECONDITION_FAILED = 412
  val REQUEST_ENTITY_TOO_LARGE = 413
  val REQUEST_URI_TOO_LONG = 414
  val UNSUPPORTED_MEDIA_TYPE = 415
  val REQUESTED_RANGE_NOT_SATISFIABLE = 416
  val EXPECTATION_FAILED = 417
  val UNPROCESSABLE_ENTITY = 422
  val LOCKED = 423
  val FAILED_DEPENDENCY = 424
  val TOO_MANY_REQUEST = 429

  val INTERNAL_SERVER_ERROR = 500
  val NOT_IMPLEMENTED = 501
  val BAD_GATEWAY = 502
  val SERVICE_UNAVAILABLE = 503
  val GATEWAY_TIMEOUT = 504
  val HTTP_VERSION_NOT_SUPPORTED = 505
  val INSUFFICIENT_STORAGE = 507
}
