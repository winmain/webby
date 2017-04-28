package webby.mvc.script
import webby.api.controllers.StaticCtl
import webby.api.mvc.Handler
import webby.route.v2.BaseRoute

/**
  * Routes to server javascript source maps
  *
  * Example usage:
  * {{{
  *   trait RouteMainTrait[R] extends BaseRoute[R] with StdRouteJsSourceMapTrait[R] {
  *     ...
  *   }
  * }}}
  */
trait StdRouteJsSourceMapTrait[R] extends BaseRoute[R] {
  // @formatter:off
  def _jsSourceMap(file: String): R =                             get"/js-sourcemap/$file<.*>"
  // @formatter:on
}

/**
  * Handlers for routes [[StdRouteJsSourceMapTrait]].
  *
  * Example usage:
  * {{{
  *   object HandlersMain extends RouteHandlers with RouteMainTrait[Handler] with StdRouteJsSourceMapHandlers {
  *     override def jsSourceMapRestriction = StaticCtl.CookieRestriction(...)
  *     ...
  *   }
  * }}}
  */
trait StdRouteJsSourceMapHandlers extends StdRouteJsSourceMapTrait[Handler] {
  def jsSourceMapRestriction: StaticCtl.ResourceRestriction

  override def _jsSourceMap(file: String): Handler = StaticCtl.atResource(file, "js-sourcemap/", "target/asset-resources/js-sourcemap", jsSourceMapRestriction)
}
