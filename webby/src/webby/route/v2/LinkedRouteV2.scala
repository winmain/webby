package webby.route.v2

import java.lang.reflect.{InvocationTargetException, Method}

import io.netty.handler.codec.http.HttpMethod
import jregex.{Matcher, Pattern}
import webby.api.mvc.Handler
import webby.route.{DomainProvider, Var}

class LinkedRouteV2(val name: String,
                    val domainProvider: DomainProvider[_],
                    val method: HttpMethod,
                    val basePath: String,
                    val pattern: Pattern,
                    routeLinksForDomainData: (Any) => RouteHandlers,
                    val vars: Vector[Var[_]],
                    val varIndices: Vector[Int],
                    val linkMethod: Method) {
  require(!vars.contains(null), "All variables must be used in url. " + toString)
  require(varIndices.size == linkMethod.getParameterTypes.size, "Method argument count mismatch. Seems method with the same name already exists. You should rename it, or make it final. " + toString)

  def resolve(domain: Any, m: Matcher): Option[Handler] = {
    val routeLinks = routeLinksForDomainData(domain)
    var i: Int = 0
    val ln = varIndices.size
    val args = Array.ofDim[AnyRef](ln)
    while (i < ln) {
      val group: String = m.group(varIndices(i) + 1)
      val vr: Var[_] = vars(i)
      try {
        args(i) = vr.fromString(group).asInstanceOf[AnyRef]
      } catch {
        // Ошибка при разборке маршрута (как правило, это NumberFormatException, когда число слишком длинное для int).
        case e: Throwable => return None
      }
      i += 1
    }
    try {
      Some(linkMethod.invoke(routeLinks, args: _*).asInstanceOf[Handler])
    } catch {
      case e: InvocationTargetException => throw e.getCause
    }
  }

  override def toString: String = method.name() + " (" + basePath + ") " + pattern + " - " + name
}
