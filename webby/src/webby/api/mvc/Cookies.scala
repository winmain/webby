package webby.api.mvc

import java.net.URLDecoder
import java.util
import java.util.Collections

import io.netty.handler.codec
import io.netty.handler.codec.http.cookie.ServerCookieDecoder

/**
  * An HTTP cookie.
  *
  * @param name     the cookie name
  * @param value    the cookie value
  * @param maxAge   The cookie expiration date in seconds.
  *                 If Long.MinValue is specified, this cookie will be removed when the browser is closed.
  *                 If an age of 0 is specified, this cookie will be automatically removed by browser because it will expire immediately.
  * @param path     the cookie path, defaulting to the root path `/`
  * @param domain   the cookie domain
  * @param secure   whether this cookie is secured, sent only for HTTPS requests
  * @param httpOnly whether this cookie is HTTP only, i.e. not accessible from client-side JavaScipt code
  */
case class Cookie(name: String,
                  value: String,
                  maxAge: Long = Cookies.BrowserSessMaxAge,
                  path: String = "/",
                  domain: String = null,
                  secure: Boolean = false,
                  httpOnly: Boolean = true)
  extends codec.http.cookie.Cookie {

  override def setWrap(wrap: Boolean): Unit = throw new UnsupportedOperationException
  override def wrap(): Boolean = false
  // Не уверен, что false здесь сойдёт, возможно это стоит переделать
  override def setHttpOnly(httpOnly: Boolean): Unit = throw new UnsupportedOperationException
  override def isHttpOnly: Boolean = httpOnly
  override def setSecure(secure: Boolean): Unit = throw new UnsupportedOperationException
  override def isSecure: Boolean = secure
  override def setMaxAge(maxAge: Long): Unit = throw new UnsupportedOperationException
  override def setPath(path: String): Unit = throw new UnsupportedOperationException
  override def setDomain(domain: String): Unit = throw new UnsupportedOperationException
  override def setValue(value: String): Unit = throw new UnsupportedOperationException
  override def compareTo(o: codec.http.cookie.Cookie): Int = throw new UnsupportedOperationException
}

/**
  * The HTTP cookies set.
  */
trait Cookies {
  def contains(name: String): Boolean
  def get(name: String): Option[String]
  def apply(name: String): String = get(name).getOrElse(sys.error("Cookie doesn't exist"))
  def asMap: Map[String, String]
}

/**
  * Реализация Cookies
  *
  * @param headerText Текст хедера с куками.
  */
class CookiesImpl(headerText: String) extends Cookies {
  import scala.collection.JavaConverters._

  val cookies: Map[String, String] = {
    val b = Map.newBuilder[String, String]
    val cookieSet: util.Set[codec.http.cookie.Cookie] =
      try ServerCookieDecoder.LAX.decode(headerText)
      catch {
        case e: IllegalArgumentException => Collections.emptySet()
      }
    for (c <- cookieSet.asScala) {
      try {
        b += ((c.name(), URLDecoder.decode(c.value(), "utf-8")))
      } catch {
        case e: IllegalArgumentException => // just skip invalid cookies
      }
    }
    b.result()
  }

  override def contains(name: String): Boolean = cookies.contains(name)
  override def get(name: String): Option[String] = cookies.get(name)
  override def toString = cookies.toString()
  override def asMap: Map[String, String] = cookies
}

/**
  * Helper utilities to encode Cookies.
  */
object Cookies {

  val DeleteMaxAge = 0L
  val BrowserSessMaxAge = Long.MinValue

  // We use netty here but just as an API to handle cookies encoding

  object empty extends Cookies {
    override def contains(name: String): Boolean = false
    override def get(name: String): Option[String] = None
    override def asMap: Map[String, String] = Map.empty
  }

  /**
    * Extract cookies from the Cookie header.
    */
  def fromHeader(header: Option[String]): Cookies = header match {
    case Some(headerText) => new CookiesImpl(headerText)
    case None => empty
  }

  /**
    * A cookie to be discarded.  This contains only the data necessary for discarding a cookie.
    *
    * @param name   the name of the cookie to discard
    * @param path   the path of the cookie, defaults to the root path
    * @param domain the cookie domain
    * @param secure whether this cookie is secured
    */
  def discarding(name: String, path: String = "/", domain: String = null, secure: Boolean = false) =
    Cookie(name, "", maxAge = 0, path = path, domain = domain, secure = secure)
}
