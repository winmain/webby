package webby.commons.text.validator

import java.net.{MalformedURLException, URL}

import org.apache.commons.lang3.StringUtils

object UrlValidator {
  /**
   * Проверить url
   */
  def validate(givenUrl: String,
               allowedSchemes: Iterable[String] = Array("http", "https")): Option[URL] = try {
    val url: URL = new URL(givenUrl)
    url.getProtocol match {
      case null | "" => return None
      case proto => if (!allowedSchemes.exists(_ == proto)) return None
    }
    url.getHost match {
      case null | "" => None
      case domain =>
        if (domain.length < 5 || !StringUtils.contains(domain, '.')) return None
        Some(url)
    }
  } catch {
    case e: MalformedURLException => None
  }

  /**
   * Проверить домен урла. Валидным считается как домен из списка allowedDomains, так и его поддомен.
   */
  def validateDomain(url: URL, allowedDomains: Iterable[String]): Boolean = {
    val host = url.getHost
    allowedDomains.exists {allowedDomain =>
      allowedDomain == host ||
        (host.length > allowedDomain.length && host.endsWith(allowedDomain) && host.charAt(host.length - allowedDomain.length - 1) == '.')
    }
  }
}
