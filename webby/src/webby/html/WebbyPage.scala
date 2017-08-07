package webby.html
import java.time.{LocalDate, ZoneId}

import com.google.common.net.HttpHeaders
import webby.commons.text.SB
import webby.commons.time.StdDates
import webby.mvc.ActTrait
import webby.mvc.script.{JsClassHolder, LessClassHolder, SassClassHolder}

import scala.util.Try
import scala.util.matching.Regex

trait WebbyPage extends ActTrait {
  val now: Long = System.currentTimeMillis()
  lazy val today: LocalDate = StdDates.toLocalDate(now, ZoneId.systemDefault())

  def scripts: PageScripts

  val headTags: SB = new SB()

  def addClassLess()(implicit holder: LessClassHolder): Unit = headTags + "<style>" + holder.lessResHolder.get + "</style>"
  def addClassSass()(implicit holder: SassClassHolder): Unit = headTags + "<style>" + holder.sassResHolder.get + "</style>"
  def addClassJsToScripts()(implicit holder: JsClassHolder): Unit = scripts.addCode(holder.jsResHolder.get)

  def mobile: Boolean
  def desktop: Boolean = !mobile

  lazy val userAgent: String = headers.get(HttpHeaders.USER_AGENT, "")

  /** Major version of Internet Explorer (example: 6, 7, 8). For non-IE browser this will be None. */
  lazy val ieVersion: Option[Int] = WebbyPage.getMajorVersion(WebbyPage.ieVersion, userAgent)

  lazy val isAndroid: Boolean = userAgent.contains("Android ")

  lazy val webkitVersion: Option[Int] = WebbyPage.getMajorVersion(WebbyPage.webkitVersion, userAgent)

  /**
    * Chrome browser version
    */
  lazy val chromeVersion: Option[Int] = WebbyPage.getMajorVersion(WebbyPage.chromeVersion, userAgent)

  /**
    * Версия Firefox. Пока что нужна только для локалки для включения ECMAScript6.
    */
  lazy val firefoxVersion: Option[Int] = WebbyPage.getMajorVersion(WebbyPage.firefoxVersion, userAgent)

  /**
    * Android version (example: 4.4 or 5.1)
    * WebView in Android apps prior to 4.4 doesn't tell Chrome version in User-agent string
    */
  lazy val androidVersion: Option[(Int, Int)] = WebbyPage.getVersion(WebbyPage.androidVersion, userAgent)

  lazy val isGoogleBot: Boolean = WebbyPage.googleBot.pattern.matcher(userAgent).find()

  /**
    * Old Android Browser (not Chrome) with transition effects and svg does not work well or not work at all.
    * This is all built-in Android browsers prior to 4.4. On 4.4 they was replaced by Chrome 33.
    */
  def oldWebkit: Boolean = webkitVersion.fold(false)(_ < 537)
  /**
    * The very old Android browsers prior to 4.0 (branch 2.x). They have problems with scroll position:fixed.
    */
  def veryOldWebkit: Boolean = webkitVersion.fold(false)(_ < 534)

  /**
    * Chrome prior to 34 dispatches event `popstate` on page start.
    * Also Android prior to 4.4 does not tell Chrome version in User-agent string.
    */
  def popstateOnPageLoad: Boolean = chromeVersion.exists(_ < 34) || androidVersion.exists(_._1 <= 4)

  def androidPriorTo_4_4: Boolean = androidVersion.exists(v => v._1 < 4 || (v._1 == 4 && v._2 < 4))
}

object WebbyPage {
  private val ieVersion = "MSIE (\\d+)".r
  private val webkitVersion = "AppleWebKit/(\\d+)".r
  private val chromeVersion = "Chrome/(\\d+)".r
  private val firefoxVersion = "Firefox/(\\d+)".r
  private val androidVersion = "Android (\\d+)\\.(\\d+)".r
  private val googleBot = "Google(?:bot| Page Speed)".r

  private def getMajorVersion(pattern: Regex, userAgent: String): Option[Int] = {
    Try(pattern.findFirstMatchIn(userAgent).map(_.group(1).toInt)).getOrElse(None)
  }
  private def getVersion(pattern: Regex, userAgent: String): Option[(Int, Int)] = {
    Try(pattern.findFirstMatchIn(userAgent).map(m => (m.group(1).toInt, m.group(2).toInt))).getOrElse(None)
  }
}
