package webby.mvc.script

import webby.api.controllers.StaticCtl
import webby.api.mvc.Handler
import webby.route.v2.BaseRoute

/**
  * Роуты для стандартного расположения ассетов.
  *
  * Пример использования:
  * {{{
  *   trait RouteMainTrait[R] extends BaseRoute[R] with StdRouteAssetsTrait[R] {
  *     ...
  *   }
  * }}}
  */
trait StdRouteAssetsTrait[R] extends BaseRoute[R] {
  // ----- Dev static -----
  def _assetsCss(file: String): R =                               get"/assets/css/$file<.*>"
  def _assetsJs(file: String): R =                                get"/assets/js/$file<.*>"
  def _assetsJsSimple(file: String): R =                          get"/assets/js-simple/$file<.*>"
  def _assetsJsGcc(file: String): R =                             get"/assets/js-gcc/$file<.*>"
  def _assetsProfile(file: String): R =                           get"/assets/profiles/$file<.*>"
  final def _assets(file: String): R =                            get"/assets/$file<.*>"
  def _public(file: String): R =                                  get"/public/$file<.*>"
}

/**
  * Обрабочики для роутов [[StdRouteAssetsTrait]].
  * Но без _assetsCss метода, который следует выбрать в одном из наследников этого трейта.
  *
  * Пример использования:
  * {{{
  *   object HandlersMain extends RouteHandlers with RouteMainTrait[Handler] with StdRouteAssetsHandlers {
  *     override object RouteUtils extends StdScriptRouteUtils(Paths, GoogleClosureServers.builder(_).build)
  *     ...
  *   }
  * }}}
  */
trait StdRouteAssetsHandlers extends StdRouteAssetsTrait[Handler] {
  def RouteUtils: StdScriptRouteUtils

  // ----- Dev static -----
  override def _assetsJs(file: String): Handler = RouteUtils.jsServer(file)
  override def _assetsJsSimple(file: String): Handler = RouteUtils.jsSimpleServer(file)
  override def _assetsJsGcc(file: String): Handler = RouteUtils.jsGccServer(file)
  override def _assetsProfile(file: String): Handler = StaticCtl.at("/app/assets/profiles", file)
  override def _public(file: String): Handler = StaticCtl.at("public", file)
}

/**
  * Example usage:
  * {{{
  *   object HandlersMain extends RouteHandlers with RouteMainTrait[Handler] with StdRouteAssetsUseSassHandlers {
  *     override object RouteUtils extends StdScriptRouteUtils(Paths, GoogleClosureServers.builder(_).build)
  *     ...
  *   }
  * }}}
  */
trait StdRouteAssetsUseSassHandlers extends StdRouteAssetsHandlers {
  override def _assetsCss(file: String): Handler = RouteUtils.sassServer(file)
}

/**
  * Example usage:
  * {{{
  *   object HandlersMain extends RouteHandlers with RouteMainTrait[Handler] with StdRouteAssetsUseLessHandlers {
  *     override object RouteUtils extends StdScriptRouteUtils(Paths, GoogleClosureServers.builder(_).build)
  *     ...
  *   }
  * }}}
  */
trait StdRouteAssetsUseLessHandlers extends StdRouteAssetsHandlers {
  override def _assetsCss(file: String): Handler = RouteUtils.lessServer(file)
}
