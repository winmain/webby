package webby.route.v2

import webby.route.{BasePathSplitter, DomainProvider}

trait RouteConfigV2 {

  def basePathSplitter: BasePathSplitter
  def routePacks: Array[RoutePack[_]]

  def parse = new ParsedRouteConfigV2(this)
}

class RoutePack[DD](val routeHandlersForDomainData: (DD) => RouteHandlers,
                    val sampleDomainData: DD,
                    val routeRoute: RouteRoute,
                    val domainProvider: DomainProvider[DD])

object RoutePack {
  def apply[DD](routeHandlersForDomainData: (DD) => RouteHandlers,
                sampleDomainData: DD,
                routeRoute: RouteRoute,
                domainProvider: DomainProvider[DD]) =
    new RoutePack[DD](routeHandlersForDomainData, sampleDomainData, routeRoute, domainProvider)

  def apply(routeHandlers: RouteHandlers, routeRoute: RouteRoute, domainProvider: DomainProvider[Any]) =
    new RoutePack[Any](_ => routeHandlers, null.asInstanceOf[Any], routeRoute, domainProvider)
}

class ParsedRouteConfigV2(val config: RouteConfigV2) {

  def basePathSplitter: BasePathSplitter = config.basePathSplitter

  def routes: Seq[LinkedRouteV2] = config.routePacks.flatMap(rp => RouteV2Parser.parse(rp, config.basePathSplitter))

  def createRequestHandler = new RequestHandlerV2(this)
}