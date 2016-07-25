package webby.route.v1

import java.util.regex.Matcher

import io.netty.handler.codec.http.HttpMethod
import webby.api.mvc.{Handler, RequestHeader}
import webby.route.DomainProvider

import scala.collection.mutable

/**
 * Оптимизированный обработчик запросов
 */
class RequestHandlerV1(val config: RouteConfigV1) {

  import config.basePathSplitter

  // Оптимизированное хранение маршрутов по схеме DomainProvider -> basePath -> routes
  val m: Vector[(DomainProvider[_], Map[String, Vector[LinkedRouteV1]])] = {
    val m = mutable.Buffer[(DomainProvider[_], mutable.Map[String, mutable.Buffer[LinkedRouteV1]])]()
    for (route <- config.routes) {
      val domainProvider = route.domainProvider
      val basePath = route.basePath
      val inDomain = (m.find(_._1 == domainProvider) match {
        case Some(row) => row
        case None =>
          val row: (DomainProvider[_], mutable.Map[String, mutable.Buffer[LinkedRouteV1]]) =
            (domainProvider, mutable.Map[String, mutable.Buffer[LinkedRouteV1]]())
          m += row
          row
      })._2
      val inBasePath = inDomain.getOrElseUpdate(basePath, mutable.Buffer[LinkedRouteV1]())
      inBasePath += route
    }
    // Сконвертировать mutable => immutable
    m.view.map(row => (row._1, row._2.view.map(rr => rr._1 -> rr._2.toVector).toMap)).toVector
  }

  def handle(domain: String, method: HttpMethod, path: String): Option[Handler] = {
    for (row <- m) {
      row._1.fromDomain(domain) match {
        case Some(domainObj) =>
          //val decodedPath = URLDecoder.decode(path, "utf-8") // Для чтения и разбора русских урлов
          val (basePath, mainPath) = basePathSplitter(path)
          return row._2.get(basePath) match {
            case Some(inBasePath) =>
              var matcher: Matcher = null
              inBasePath.find {
                route: LinkedRouteV1 =>
                  if (route.method eq method) {
                    matcher = route.pattern.matcher(mainPath)
                    matcher.matches()
                  } else false
              } match {
                case Some(route) => Some(route.resolve(domainObj.asInstanceOf[AnyRef], matcher))
                case _ => None
              }
            case None => None
          }
        case None => None
      }
    }
    None
  }

  def handle(request: RequestHeader): Option[Handler] = handle(request.domain, request.method, request.path)
}