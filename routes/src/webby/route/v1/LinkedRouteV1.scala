package webby.route.v1

import java.util.regex.{Matcher, Pattern}

import io.netty.handler.codec.http.HttpMethod
import webby.api.mvc.Handler
import webby.route.DomainProvider

/**
 * Маршрут, привязанный к Handler'у.
 */
trait LinkedRouteV1 {
  val name: String
  val domainProvider: DomainProvider[_]
  val method: HttpMethod
  val basePath: String
  val pattern: Pattern

  def resolve(domain: Any, m: Matcher): Handler
}
