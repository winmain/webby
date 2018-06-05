package webby.adm
import org.apache.commons.codec.digest.DigestUtils
import querio._
import webby.adm.view.StdAdmLoginView
import webby.api.App
import webby.api.libs.CryptoSigner
import webby.api.mvc.Results._
import webby.api.mvc.{PlainResult, _}
import webby.commons.cache.table.RecordsCache
import webby.route.v2.Route

/**
  * This is an all-in-one config for admin pages & session support.
  *
  * While implementing this trait you can override any property or method.
  *
  * Standard steps to properly implement admin pages:
  *
  * :: 1. Create 3 database tables with structures to satisfy and implement this traits:
  * [[StaffTrait]]
  * [[StaffRoleTrait]]
  * [[StaffSessTrait]]
  *
  * :: 2. Create Permission enum, see [[OnePermission]]
  *
  * :: 3. Add [[StdRouteAdmTrait]] to your RouteAdm routes (see [[StdRouteAdmTrait]])
  *
  * :: 4. Implement Adm object:
  * {{{
  * object Adm extends AdmTrait {
  *   override type Staff = AdmStaff
  *   override type StaffTable = AdmStaffTable
  *
  *   override type StaffRole = AdmStaffRole
  *   override type StaffRoleTable = AdmStaffRoleTable
  *
  *   override type StaffSess = AdmStaffSess
  *   override type MutableStaffSess = MutableAdmStaffSess
  *   override type StaffSessTable = AdmStaffSessTable
  *
  *   override def db: DbTrait = Db
  *
  *   override def staffTable = AdmStaff
  *   override def staffRoleTable = AdmStaffRole
  *   override def staffSessTable = AdmStaffSess
  *   override def route = RouteAdm
  *
  *   override def loginTitle: String = "My admin"
  * }
  * }}}
  *
  * :: 5. Add task to your Quartz configuration to weed staff sessions:
  * {{{
  * class Quartz ... {
  *   ...
  *   s(hourly(10), job("weed", "Adm.staffSessContainer.weed()", c => Adm.staffSessContainer.weed()))
  *   ...
  * }
  * }}}
  */
trait AdmTrait extends StdStaffCaches { adm =>
  // Staff table
  type Staff <: StaffTrait
  type MutableStaff <: MutableStaffTrait[Staff]
  type StaffTable <: StaffTableTrait with Table[Int, Staff, MutableStaff]

  // StaffRole table
  type StaffRole <: StaffRoleTrait
  type StaffRoleTable <: StaffRoleTableTrait with TrTable[Int, StaffRole]

  // StaffSess table
  type StaffSess <: StaffSessTrait
  type MutableStaffSess <: MutableStaffSessTrait[StaffSess]
  type StaffSessTable <: StaffSessTableTrait with Table[Int, StaffSess, MutableStaffSess]

  def tokenSecret: String = App.app.configuration.getString("adm.secret").getOrElse(sys.error("Missing adm.secret config"))
  val tokenSigner = new CryptoSigner(tokenSecret.getBytes)

  /** The session timeout in seconds */
  def sessionTimeoutInSeconds: Int = 3600 * 24 * 2

  /** Cookie name used for redirect after admin login */
  def accessUriCookieName = "access_uri"

  /** Whether use the secure option or not use it in the cookie. */
  def sessCookieSecureOption: Boolean = App.isProd

  /** Cookie name for session tokens */
  def sessCookieName: String = "adm-sess-id"

  /** Cookie domain for session tokens */
  def sessCookieDomain: String = null


  /** If the user is not logged in and tries to access a protected resource then redirect them as follows: */
  def unauthorized(request: RequestHeader): PlainResult =
    Ok(loginView()).withCookie(Cookie(accessUriCookieName, request.path))

  /** If authorization failed (usually incorrect password) redirect the user as follows: */
  def authorizationFailed(request: RequestHeader, permission: PermissionSet): PlainResult =
    Forbidden("Need permission: " + permission.dbValues)


  /**
    * Retrieve staff session from request.
    */
  def getSess(implicit request: RequestHeader): Option[StaffSess] = {
    val now = System.currentTimeMillis()
    for {
      cookie <- request.cookies.get(sessCookieName)
      token <- tokenSigner.verifyComposedLong(cookie)
      staffSess <- staffSessContainer.get(now, token)
    } yield {
      if (isMainHost) staffSessContainer.setTimeout(now, staffSess, sessionTimeoutInSeconds)
      else staffSess
    }
  }

  /**
    * Retrieve [[StaffSess]] from Request if staff signed in and has specified `permission`.
    * Returns None if staff not signed in or does not have permission.
    */
  def authorized(permission: PermissionSet)(implicit request: RequestHeader): Option[StaffSess] =
    getSess(request).filter(_.canAccessTo(permission))

  /**
    * Retrieve [[StaffSess]] from Request or returns special banner page.
    * Returns Right([[StaffSess]]) if staff signed in and has specified `permission`.
    * Returns Left([[PlainResult]]) with special page otherwise.
    */
  def authorizeOrFail(permission: PermissionSet)(implicit request: RequestHeader): Either[PlainResult, StaffSess] = {
    getSess(request) match {
      case None => Left(unauthorized(request))
      case Some(sess) =>
        if (sess.canAccessTo(permission)) Right(sess)
        else Left(authorizationFailed(request, permission))
    }
  }

  /**
    * Calculate password hash for [[StaffTrait.hashedPassword]]
    */
  def passwordHash(password: String): String = DigestUtils.md5Hex(password)

  // ------------------------------- Hosts & domains -------------------------------

  def isMainHost = true

  // ------------------------------- Database -------------------------------

  def db: DbTrait

  // ------------------------------- Staff, staff roles -------------------------------

  def staffTable: StaffTable
  def staffRoleTable: StaffRoleTable

  lazy val staffCache: RecordsCache[Int, Staff] = new StdStaffCache
  lazy val staffRoleCache: RecordsCache[Int, StaffRole] = new StdStaffRoleCache

  def findAndCheckStaff(checkPassword: Boolean)(login: String, password: String): Option[Int] = {
    val pwHash = passwordHash(password)
    staffCache.allRecords.find {staff =>
      staff.active && staff.login == login &&
        (!checkPassword || staff.hashedPassword.contains(pwHash))
    }.map(s => s.id)
  }

  // ------------------------------- StaffSess -------------------------------

  type StaffSessCache = StaffSessCacheTrait[StaffSess]
  type StaffSessContainer = StaffSessContainerTrait[StaffSess]

  def staffSessTable: StaffSessTable

  val staffSessCache: StaffSessCache = new StdStaffSessCache(this)
  val staffSessContainer: StaffSessContainer = new StdStaffSessContainer(this)

  def createNewStaffSess: MutableStaffSess = staffSessTable._newMutableRecord

  // ------------------------------- Routes & views -------------------------------

  /** Adm routes */
  def route: StdRouteAdmTrait[Route]

  def loginView(errorMessage: Option[String] = None, login: Option[String] = None) =
    new StdAdmLoginView(adm, errorMessage, login)

  /** Title for login form */
  def loginTitle: String = "Authentication"

  // ------------------------------- Session actions -------------------------------

  /**
    * Starts new session for specified `staffId`.
    *
    * @param staffId Staff id session started for
    * @param body Wrapped function body resulting [[PlainResult]] for writing session cookie to
    */
  def newSession(staffId: Int)(body: => PlainResult): PlainResult = {
    val now = System.currentTimeMillis()
    val token: Long = staffSessContainer.startNewSession(now, staffId, sessionTimeoutInSeconds)
    val value = tokenSigner.signComposeLong(token)
    body.withCookie(Cookie(sessCookieName, value, domain = sessCookieDomain, secure = sessCookieSecureOption, httpOnly = true))
  }

  /**
    * Removes staff session and discards session cookie
    */
  def logout(result: => PlainResult)(implicit request: RequestHeader): PlainResult = {
    for {cookieValue <- request.cookies.get(sessCookieName)
         token <- tokenSigner.verifyComposedLong(cookieValue)} {
      staffSessContainer.remove(token)
    }
    result.withCookie(Cookies.discarding(sessCookieName, domain = sessCookieDomain, secure = sessCookieSecureOption))
  }

  // ------------------------------- Standard actions controller -------------------------------

  /**
    * Show login page
    */
  def loginAction = SimpleAction {req =>
    Ok(loginView())
  }

  /**
    * Return the `gotoLoginSucceeded` method's result in the login action.
    *
    * Since the `gotoLoginSucceeded` returns `PlainResult`,
    * you can add a procedure like the `gotoLogoutSucceeded`.
    */
  def loginPostAction = SimpleAction.withBodyParser(BodyParsers.formUrlEncoded) {(rh, request) =>
    val login = request.get("login", "")
    val password = request.get("password", "")
    if (login.isEmpty) BadRequest.html(loginView(errorMessage = Some("No login")).result)
    else {
      findAndCheckStaff(checkPassword = App.isProd)(login, password) match {
        case Some(ss) =>
          newSession(ss) {
            (rh.cookies.get(accessUriCookieName) match {
              case None => Redirect(route.main)
              case Some(uri) => Found(uri)
            }).withCookie(Cookies.discarding(accessUriCookieName))
          }
        case None =>
          BadRequest.html(loginView(errorMessage = Some("Invalid login or password"), login = Some(login)).result)
      }
    }
  }

  /**
    * Logout action
    */
  def logoutAction = SimpleAction {implicit req =>
    logout(Redirect(route.main))
  }
}
