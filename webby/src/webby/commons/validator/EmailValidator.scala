package webby.commons.validator

import javax.mail.internet.{AddressException, InternetAddress}

import org.slf4j.LoggerFactory
import webby.api.App
import webby.commons.text.StringWrapper.wrapper

/**
  * Requires sbt dependencies
  * {{{
  *   deps += "commons-validator" % "commons-validator" % "1.5.1"
  *   deps += "org.apache.commons" % "commons-email" % "1.4"
  * }}}
  */
object EmailValidator {
  /**
   * Проверка на валидный email через `exim`.
   * Это наиболее надёжная проверка, но она может занять некоторое время при вызове `exim`.
   */
  def isValid(email: String): Boolean = {
    isValidInternetAddress(email) &&
      (if (App.isProd) {
        import scala.sys.process._
        Seq("/usr/sbin/exim", "-bv", email).!(new ProcessLogger {
          override def buffer[T](f: => T): T = f
          override def out(s: => String): Unit = {}
          override def err(sFn: => String): Unit = sFn match {
            case s if s.contains("remote host address is the local host") => // do nothing
            case s => LoggerFactory.getLogger(getClass).warn("Exim check email <" + email + "> error: " + s)
          }
        }) match {
          case 0 => true
          case _ => false
          // 1: cannot be resolved at this time: host lookup did not complete
          // 2: failed to verify: Unrouteable address
        }
      } else true) // на локалке у нас нет exim, поэтому запускаем только быструю проверку
  }

  /**
   * Быстрая (но менее надёжная) проверка email.
   */
  def isValidFast(email: String): Boolean =
    isValidInternetAddress(email) && org.apache.commons.validator.routines.EmailValidator.getInstance().isValid(email)

  private val SimplePattern = "[^@\\s\\-][^@\\s]*@[^@\\s.][^@\\s]*.*\\.[^@\\s]*[^@\\s.]+".pat

  private [validator] def isValidInternetAddress(email: String): Boolean = {
    if (!SimplePattern.matches(email)) return false
    try {
      new InternetAddress(email)
      true
    } catch {
      case e: AddressException => false
    }
  }
}
