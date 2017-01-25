package webby.route.v2

import java.util.regex.Matcher

import io.netty.handler.codec.http.HttpMethod
import webby.api.mvc.{Handler, RequestHeader}
import webby.route.{BasePathSplitter, DomainProvider}

import scala.collection.{breakOut, mutable}

/**
 * Оптимизированный обработчик запросов
 */
class RequestHandlerV2(val pc: ParsedRouteConfigV2) {

  val basePathSplitter: BasePathSplitter = pc.basePathSplitter

  // Оптимизированное хранение маршрутов по схеме DomainProvider -> basePath -> routes
  val m: Vector[(DomainProvider[_], Map[String, Vector[LinkedRouteV2]])] = {
    val m = mutable.Buffer[(DomainProvider[_], mutable.Map[String, mutable.Buffer[LinkedRouteV2]])]()
    for (route <- pc.routes) {
      val domainProvider = route.domainProvider
      val basePath = route.basePath
      val inDomain = (m.find(_._1 == domainProvider) match {
        case Some(row) => row
        case None =>
          val row: (DomainProvider[_], mutable.Map[String, mutable.Buffer[LinkedRouteV2]]) =
            (domainProvider, mutable.Map[String, mutable.Buffer[LinkedRouteV2]]())
          m += row
          row
      })._2
      val inBasePath = inDomain.getOrElseUpdate(basePath, mutable.Buffer[LinkedRouteV2]())
      inBasePath += route
    }
    // Сконвертировать mutable => immutable
    m.map(row =>
      row._1 ->
        (row._2.map(rr => rr._1 -> rr._2.toVector)(breakOut): Map[String, Vector[LinkedRouteV2]])
    )(breakOut)
  }

  def handle(domain: String, method: HttpMethod, path: String): Option[Handler] = {
    for (row <- m) {
      row._1.fromDomain(domain) match {
        case Some(domainObj) =>
          //val decodedPath = URLDecoder.decode(path, "utf-8") // Для чтения и разбора русских урлов
          val (basePath, mainPath) = basePathSplitter.split(path, learning = false, hasVar = false)
          return row._2.get(basePath) match {
            case Some(inBasePath) =>
              var matcher: Matcher = null
              inBasePath.find {route: LinkedRouteV2 =>
                if (route.method eq method) {
                  matcher = route.pattern.matcher(mainPath)
                  matcher.matches()
                } else false
              } match {
                case Some(route) => route.resolve(domainObj.asInstanceOf[AnyRef], matcher)
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
