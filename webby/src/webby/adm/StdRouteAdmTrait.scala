package webby.adm
import webby.api.App
import webby.api.mvc.Handler
import webby.route.v2.BaseRoute

/**
  * Routes for default admin pages
  *
  * Example usage:
  * {{{
  *   trait RouteAdmTrait[R] extends BaseRoute[R] with StdRouteAdmTrait[R] {
  *     ...
  *   }
  * }}}
  */
trait StdRouteAdmTrait[R] extends BaseRoute[R] {
  override protected def httpsOnly: Boolean = App.isProd

  def main: R =                                                   get"/="
  def main2: R =                                                  get"/=/"
  def login: R =                                                  get"/=/login"
  def loginPost: R =                                              post"/=/login"
  def logout: R =                                                 get"/=/logout"
}

/**
  * Handlers for [[StdRouteAdmTrait]].
  *
  * Example usage:
  * {{{
  *   object HandlersAdm extends RouteHandlers with RouteAdmTrait[Handler] with StdRouteAdmHandlers {
  *     override protected def adm: AdmTrait = Adm
  *     ...
  *   }
  * }}}
  */
trait StdRouteAdmHandlers extends StdRouteAdmTrait[Handler] {
  protected def adm: AdmTrait

  override def login: Handler = adm.loginAction
  override def loginPost: Handler = adm.loginPostAction
  override def logout: Handler = adm.logoutAction
}
