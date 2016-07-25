package webby.route.v2

import io.netty.handler.codec.http.HttpMethod
import webby.api.mvc.Handler

trait BaseRoute[R] {
  import scala.language.implicitConversions

  final protected class UrlStringContext(val sc: StringContext) {
    def get(args: Any*): R = route(Seq(HttpMethod.GET), sc.parts, args)
    def post(args: Any*): R = route(Seq(HttpMethod.POST), sc.parts, args)
    def getPost(args: Any*): R = route(Seq(HttpMethod.GET, HttpMethod.POST), sc.parts, args)

    /**
     *  Для кросс-доменных запросы
     *  В CRM есть окна, которые запрашиваются с основного сервера
     */
    def getPostOptions(args: Any*): R = route(Seq(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS), sc.parts, args)

    /**
     * Кросс-доменный POST
     * Нужен чтобы в CRM GET-запрос проходил с moon, а пост уходил на основной домен
     * Тем самым мы снижаем нагрузку на основной сервер
     */
    def postOptions(args: Any*): R = route(Seq(HttpMethod.POST, HttpMethod.OPTIONS), sc.parts, args)

    def getOptions(args: Any*): R = route(Seq(HttpMethod.GET, HttpMethod.OPTIONS), sc.parts, args)
  }
  implicit final protected def _UrlStringContext(sc: StringContext): UrlStringContext = new UrlStringContext(sc)

  protected def toDomain: String
  protected def httpsOnly: Boolean = false
  protected def route(methods: Seq[HttpMethod], parts: Seq[String], args: Seq[Any]): R
}

abstract class RouteRoute extends BaseRoute[Route] {
  import scala.language.implicitConversions

  protected override final def route(methods: Seq[HttpMethod], parts: Seq[String], args: Seq[Any]): Route = new Route(methods, toDomain, parts, args, httpsOnly = httpsOnly)
}

abstract class RouteHandlers extends BaseRoute[Handler] {
  import scala.language.implicitConversions

  protected override final def route(methods: Seq[HttpMethod], parts: Seq[String], args: Seq[Any]): Handler = sys.error("Method cannot be implemented")
}
