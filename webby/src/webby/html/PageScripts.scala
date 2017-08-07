package webby.html
import org.intellij.lang.annotations.Language
import webby.api.App
import webby.commons.collection.IterableWrapper.wrapIterable
import webby.commons.text.SB
import webby.html.PageScripts.JsPart
import webby.mvc.script.minifier.StdJsMinifier

import scala.collection.mutable.ArrayBuffer

trait JsCodeAppender {
  def addCode(@Language("JavaScript") script: String): Unit
  def addCode(script: SB): Unit
  def addCode(script: StdHtmlView): Unit

  def prependCode(@Language("JavaScript") script: String): Unit
}

/**
  * Special class suited for defining scripts and dependencies at the end of a page.
  * Accumulated code and tags inserts on page via calling [[printForPage()]] method.
  */
abstract class PageScripts extends JsCodeAppender {
  protected val jsParts: ArrayBuffer[JsPart] = new ArrayBuffer[JsPart](4)
  /** Optional jsParts. They will be loaded, but jsOnLoad does not require them to complete. */
  protected val optJsParts: ArrayBuffer[JsPart] = new ArrayBuffer[JsPart](4)
  protected val tags: StringBuilder = new StringBuilder()
  protected val uniqueExternalScriptUrls: ArrayBuffer[String] = new ArrayBuffer[String](4)

  protected var _onLoadCode: StringBuilder = null
  protected def code: StringBuilder = {
    if (_onLoadCode == null) _onLoadCode = new StringBuilder()
    _onLoadCode
  }

  /** Include JsPart-script dependency */
  def addJsPart(jsPartName: String, url: String, version: Int = 0, optional: Boolean = false): Unit = {
    (if (optional) optJsParts else jsParts) += JsPart(jsPartName, if (version == 0) url else url + "?" + version)
  }

  /** Add external asynchronous script */
  def addExternalScript(rawUrl: String) {
    tags append "<script src=\"" append rawUrl append "\" async></script>"
  }

  /** Add external asynchronous script only once */
  def addExternalScriptOnce(rawUrl: String): Unit = {
    if (!uniqueExternalScriptUrls.contains(rawUrl)) {
      addExternalScript(rawUrl)
      uniqueExternalScriptUrls += rawUrl
    }
  }

  override def addCode(@Language("JavaScript") script: String): Unit = code append script append ';'
  override def addCode(script: SB): Unit = code append script.sb append ';'
  override def addCode(script: StdHtmlView): Unit = code append script.buf.sb append ';'

  override def prependCode(@Language("JavaScript") script: String): Unit = code.insert(0, script)

  def addTags(htmlView: StdHtmlView): Unit = tags append htmlView.result
  def addTags(body: String): Unit = tags append body

  /**
    * Insert accumulated tags and code on page.
    */
  //def printForPage(restPart: Option[Public.ScriptClosure] = None)(implicit buf: HtmlBuffer, page: PageTrait): Unit = {
  def printForPage()(implicit buf: HtmlBuffer, page: WebbyPage): Unit = {
    // jsParts
    buf + "<script>jsParts={"
    jsParts.foreachWithSep(buf + '"' + _.name + '"' + ":0", buf + ',')
    buf + "};"
    (jsParts ++ optJsParts).foreachWithSep(jsPart => buf + "importJsPart(\"" + jsPart.name + "\",\"" + jsPart.url + "\")", buf + ";")
    buf + "</script>"

    buf + tags
    buf + "\n<script>"
    //restPart.foreach(buf ++ _.restPart)
    if (_onLoadCode != null) buf + "jsOnLoad(function(){" + code + "})"
    buf + "</script>"
  }

  def getForExec: String = code.toString
}

object PageScripts {
  case class JsPart(name: String, url: String)
}

/**
  * Common usage:
  * {{{
  *   object Scripts {
  *     private val commonPrepender = new PageScriptsCommonPrepender("assets/js/raw/prepend.js", "assets/js/raw/prepend-dev.js")
  *
  *     def prependForPage()(implicit buf: HtmlBuffer): Unit = commonPrepender.prependForPage()
  *   }
  * }}}
  *
  * @param jsPathFromApp    Local path to prepend.js file
  * @param jsPathFromAppDev Local path to additional prepend-dev.js file only for [[App.isDev]] mode
  */
class PageScriptsCommonPrepender(jsPathFromApp: String, jsPathFromAppDev: String = null) {
  val fixedPrependJsBody: String = StdJsMinifier.load(jsPathFromApp)

  def prependJsBody: String = {
    if (App.isDev) {
      if (jsPathFromAppDev == null) {
        StdJsMinifier.load(jsPathFromApp)
      } else {
        StdJsMinifier.load(jsPathFromApp) + ";" + StdJsMinifier.load(jsPathFromAppDev)
      }
    } else {
      fixedPrependJsBody
    }
  }

  def prependForPage()(implicit buf: HtmlBuffer): Unit = {
    buf ++ "<script>" ++ prependJsBody ++ "</script>"
  }
}
