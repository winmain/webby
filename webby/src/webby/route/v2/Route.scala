package webby.route.v2

import javax.annotation.Nullable

import io.netty.handler.codec.http.HttpMethod
import webby.commons.io.{CommonUrl, Url}
import webby.commons.text.SB

class Route(val methods: Seq[HttpMethod], val domain: String, val parts: Seq[String], val args: Seq[Any], val httpsOnly: Boolean) extends Url {
  @Nullable override def protocol: String = if (httpsOnly) "https" else null
  override def path: String = pathSb.toString

  override def pathToSb(sb: SB): SB = {
    val pi = parts.iterator
    val ai = args.iterator
    sb.sb append pi.next()
    while (ai.hasNext) {
      sb.sb append ai.next
      sb.sb append stripRegexp(pi.next())
    }
    sb
  }

  override def copyPath(newPath: String): Url = new CommonUrl(protocol, domain, newPath)

  def urlNoArgs: Url = copyPath(pathNoArgs)

  /**
    * Вернуть локальный путь этого маршрута без домена, и без подстановки входных переменных args.
    */
  def pathNoArgs: String = {
    val pi = parts.iterator
    val sb = new java.lang.StringBuilder(64)
    sb append pi.next()
    while (pi.hasNext) {
      sb append stripRegexp(pi.next())
    }
    sb.toString
  }

  private def stripRegexp(part: String): String =
    if (part.length > 2 && part.charAt(0) == '<') part.substring(part.indexOf('>') + 1)
    else part
}
