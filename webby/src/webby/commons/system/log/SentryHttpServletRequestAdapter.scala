package webby.commons.system.log

import java.io.BufferedReader
import java.security.Principal
import java.util.{Collections, Locale}
import java.{util => ju}
import javax.servlet.http._
import javax.servlet.{ServletInputStream, ServletResponse, _}

import com.google.common.collect.Maps
import webby.api.mvc.RequestHeader

import scala.collection.JavaConverters._

/**
  * Адаптер, чтобы [[RequestHeader]] мог прикинуться запросом сервлета [[HttpServletRequest]].
  * Далеко не все методы реализованы. А часть из тех что реализована являются заглушками.
  */
class SentryHttpServletRequestAdapter(r: RequestHeader) extends HttpServletRequest {
  override def getRequestURL: StringBuffer = new StringBuffer(r.uri)
  override def getMethod: String = r.method.name()
  override val getParameterMap: ju.Map[String, Array[String]] = {
    val queryMap: ju.Map[String, ju.List[String]] = r.query.asJavaMultiMap
    val result = Maps.newHashMapWithExpectedSize[String, Array[String]](queryMap.size())
    for ((key, values) <- queryMap.asScala) {
      result.put(key, values.toArray(new Array[String](0)))
    }
    result
  }
  override def getQueryString: String = r.query.original

  override val getCookies: Array[Cookie] = r.cookies.asMap.map {case (name, value) => new Cookie(name, value)}(collection.breakOut)

  override def getRemoteAddr: String = r.remoteAddress
  override def getServerName: String = r.host
  override def getServerPort: Int = 80

  override def getLocalAddr: String = null
  override def getLocalName: String = null
  override def getLocalPort: Int = 80

  override def getProtocol: String = "HTTP/1.1"
  override def isSecure: Boolean = false
  override def isAsyncStarted: Boolean = false
  override def getAuthType: String = null
  override def getRemoteUser: String = null

  override def getHeaderNames: ju.Enumeration[String] = Collections.enumeration(r.headers.http.names)
  override def getHeaders(name: String): ju.Enumeration[String] = Collections.enumeration(r.headers.http.getAll(name))

  // ------------------------------- Unsupported methods -------------------------------

  override def authenticate(response: HttpServletResponse): Boolean = ???
  override def isRequestedSessionIdFromURL: Boolean = ???
  override def getParts: ju.Collection[Part] = ???
  override def getUserPrincipal: Principal = ???
  override def getPathInfo: String = ???
  override def getPart(name: String): Part = ???
  override def getContextPath: String = ???
  override def getServletPath: String = ???
  override def getRequestURI: String = ???
  override def getPathTranslated: String = ???
  override def getIntHeader(name: String): Int = ???
  override def changeSessionId(): String = ???
  override def getRequestedSessionId: String = ???
  override def logout(): Unit = ???
  override def isRequestedSessionIdFromUrl: Boolean = ???
  override def upgrade[T <: HttpUpgradeHandler](handlerClass: Class[T]): T = ???
  override def isRequestedSessionIdValid: Boolean = ???
  override def getSession(create: Boolean): HttpSession = ???
  override def getSession: HttpSession = ???
  override def getDateHeader(name: String): Long = ???
  override def isUserInRole(role: String): Boolean = ???
  override def isRequestedSessionIdFromCookie: Boolean = ???
  override def login(username: String, password: String): Unit = ???
  override def getHeader(name: String): String = ???
  override def getParameter(name: String): String = ???
  override def getRequestDispatcher(path: String): RequestDispatcher = ???
  override def startAsync(): AsyncContext = ???
  override def startAsync(servletRequest: ServletRequest, servletResponse: ServletResponse): AsyncContext = ???
  override def getRealPath(path: String): String = ???
  override def getLocale: Locale = ???
  override def getRemoteHost: String = ???
  override def getParameterNames: ju.Enumeration[String] = ???
  override def getContentLengthLong: Long = ???
  override def getAttribute(name: String): AnyRef = ???
  override def removeAttribute(name: String): Unit = ???
  override def getAsyncContext: AsyncContext = ???
  override def getCharacterEncoding: String = ???
  override def setCharacterEncoding(env: String): Unit = ???
  override def getParameterValues(name: String): Array[String] = ???
  override def getRemotePort: Int = ???
  override def getLocales: ju.Enumeration[Locale] = ???
  override def getAttributeNames: ju.Enumeration[String] = ???
  override def setAttribute(name: String, o: scala.Any): Unit = ???
  override def getDispatcherType: DispatcherType = ???
  override def getContentLength: Int = ???
  override def getContentType: String = ???
  override def getReader: BufferedReader = ???
  override def isAsyncSupported: Boolean = ???
  override def getServletContext: ServletContext = ???
  override def getScheme: String = ???
  override def getInputStream: ServletInputStream = ???
}
