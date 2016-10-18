package webby.commons.text.html

import org.apache.commons.lang3.StringEscapeUtils
import org.intellij.lang.annotations.Language
import webby.api.mvc.{PlainResult, Results}
import webby.commons.io.Url
import webby.commons.text.{Joined, SB}

import scala.language.implicitConversions

/**
  * Низкоуровневый класс, описывающий все теги.
  * Может наследоваться только классом [[StdHtmlView]].
  * Зачем он нужен, и почему их два - см. документацию к [[StdHtmlView]]
  */
abstract sealed class HtmlBase(val buf: HtmlBuffer) extends Appendable {self =>
  implicit protected def _self: HtmlBase = this
  implicit protected def _buf: HtmlBuffer = buf
  implicit protected def joinedAppenderToString(ja: Joined): String = ja.toString

  implicit sealed class HtmlStringContext(val sc: StringContext) {
    def h(args: Any*): HtmlBase = {buf ++ sc.raw(args: _*); self}
  }
  implicit sealed class HtmlStringWrapper(val s: String) {
    def unary_+ : HtmlBase = {buf ++ s; self}
    def unary_~ : HtmlBase = {buf ++ StringEscapeUtils.escapeXml10(s); self}
    /** html-комментарий */
    def unary_! : HtmlBase = {buf ++ "<!-- " ++ s ++ " -->"; self}
  }
  implicit sealed class HtmlStringOptionWrapper(val s: Option[String]) {
    def unary_+ : HtmlBase = {s.foreach(buf ++ _); self}
    def unary_~ : HtmlBase = {s.foreach(buf ++ StringEscapeUtils.escapeXml10(_)); self}
  }
  implicit sealed class HtmlTextWrapper(val t: HtmlBase) {
    def unary_+ : HtmlBase = t
  }
  implicit sealed class HtmlSBWrapper(val s: SB) {
    def unary_+ : HtmlBase = {buf ++ s.sb; self}
  }

  def ++(text: String): HtmlBase = {buf ++ text; this}
  def ++(value: Int): HtmlBase = {buf ++ value; this}
  def ++(value: Long): HtmlBase = {buf ++ value; this}

  /** Этот метод можно использовать только в теле тега script, потому что он выводит raw как есть.
    * Он нужен для IDE, чтобы включить Language Injections
    */
  def js(@Language("JavaScript") raw: String): HtmlBase = {buf ++ raw; this}
  /** Этот метод можно использовать только в теле тега script, потому что он выводит javascript код. */
  def jsVar(@Language(value = "JavaScript", prefix = "var ", suffix = ";") name: String, value: Int): Unit = buf ++ "var " ++ name ++ '=' ++ value ++ ';'
  /** Этот метод можно использовать только в теле тега script, потому что он выводит javascript код. */
  def jsVar(@Language(value = "JavaScript", prefix = "var ", suffix = ";") name: String, value: String): Unit = buf ++ "var " ++ name ++ "=\"" ++ StringEscapeUtils.escapeJson(value) ++ "\";"

  /** Тег нужен для IDE, чтобы включить Language Injections */
  def code(@Language("HTML") raw: String): HtmlBase = {buf ++ raw; this}

  def tag(tag: String): CommonTag = new CommonTag(tag, shortClose = false)
  def short(tag: String): CommonTag = new CommonTag(tag, shortClose = true)
  def e(v: String): String = StringEscapeUtils.escapeXml10(v)

  // -------------------- text helpers --------------------

  def +(v: String): HtmlBase = {buf ++ v; this}
  def +(v: Option[String]): HtmlBase = {v.foreach(buf ++ _); this}
  def +(v: Int): HtmlBase = {buf ++ v; this}
  def +(v: Long): HtmlBase = {buf ++ v; this}
  def +(v: HtmlBase): HtmlBase = this
  def +(v: BaseTag): HtmlBase = this

  // implementation of Appendable interface

  override def append(c: Char): HtmlBase = {buf ++ c; this}
  override def append(cs: CharSequence): HtmlBase = {buf append cs; this}
  override def append(cs: CharSequence, start: Int, end: Int): HtmlBase = {buf.append(cs, start, end); this}

  def ~(v: String): HtmlBase = {buf ++ StringEscapeUtils.escapeXml10(v); this}
  def ~(v: HtmlBase): HtmlBase = this

  def sp: HtmlBase = {buf ++ ' '; this}
  def sp(v: String): HtmlBase = {buf ++ ' ' ++ v; this}
  def sp(v: Int): HtmlBase = {buf ++ ' ' ++ v; this}
  def sp(v: Long): HtmlBase = {buf ++ ' ' ++ v; this}

  def nbsp: HtmlBase = {buf ++ "&nbsp;"; this}
  def nbsp(v: String): HtmlBase = {buf ++ "&nbsp;" ++ v; this}
  def nbsp(v: Int): HtmlBase = {buf ++ "&nbsp;" ++ v; this}
  def nbsp(v: Long): HtmlBase = {buf ++ "&nbsp;" ++ v; this}

  def br: HtmlBase = {buf ++ "<br>"; this}
  def br(v: String): HtmlBase = {buf ++ "<br>" ++ v; this}
  def br(v: Int): HtmlBase = {buf ++ "<br>" ++ v; this}
  def br(v: Long): HtmlBase = {buf ++ "<br>" ++ v; this}

  // -------------------- html tags --------------------

  def DOCTYPE_html: HtmlBase = {buf ++ "<!DOCTYPE html>"; this}

  def htmlTag: CommonTag = tag("html")
  def headTag: CommonTag = tag("head")
  def bodyTag: CommonTag = tag("body")

  // -------------------- head tags --------------------

  def titleTag: CommonTag = tag("title")
  def title(unsafe: String): HtmlBase = titleTag ~ unsafe

  def metaTag: StdMetaTag = new StdMetaTag
  def metaCharset(charset: String): HtmlBase = {metaTag.attr("charset", charset); this}
  def metaCharsetUtf8: HtmlBase = metaCharset("utf-8")

  def meta(name: String, rawContent: String): HtmlBase = if (rawContent != null) {metaTag.name(name).content(rawContent); this} else this
  def metaKeywords(keywords: String): HtmlBase = meta("keywords", e(keywords))
  def metaDescription(description: String): HtmlBase = meta("description", e(description))
  def metaRobots(rawContent: String): HtmlBase = meta("robots", rawContent)
  def metaViewport(rawContent: String): HtmlBase = meta("viewport", rawContent)

  def metaHttpEquiv(name: String, rawContent: String): StdMetaTag = metaTag.attr("http-equiv", name).content(rawContent)
  def metaContentType(rawContentType: String): StdMetaTag = metaHttpEquiv("Content-type", rawContentType)

  def link: CommonTag = short("link")
  def linkStylesheet(rawUrl: String): CommonTag = link.attr("rel", "stylesheet").attr("href", rawUrl)
  def linkStylesheet(media: String, rawUrl: String): CommonTag = linkStylesheet(rawUrl).attr("media", media)
  def linkStylesheet(url: Url): CommonTag = linkStylesheet(url.quotedUrl)
  def linkStylesheet(media: String, url: Url): CommonTag = linkStylesheet(media, url.quotedUrl)
  def linkShortcutIcon(url: Url): CommonTag = link.attr("rel", "shortcut icon").attr("href", url.quotedUrl)


  // -------------------- block tags --------------------

  def script: StdScriptTag = new StdScriptTag
  def style: CommonTag = tag("style")
  def style(@Language("CSS") rawCss: String): HtmlBase = style ~ rawCss
  def header: CommonTag = tag("header")
  def footer: CommonTag = tag("footer")
  def section: CommonTag = tag("section")
  def div: CommonTag = tag("div")
  def p: CommonTag = short("p")
  def pre: CommonTag = tag("pre")
  def ul: CommonTag = tag("ul")
  def ol: CommonTag = tag("ol")
  def li: CommonTag = tag("li")
  def dl: CommonTag = tag("dl")
  def dt: CommonTag = tag("dt")
  def dd: CommonTag = tag("dd")
  def hr: StdHrTag = new StdHrTag

  def h1: CommonTag = tag("h1")
  def h2: CommonTag = tag("h2")
  def h3: CommonTag = tag("h3")
  def h4: CommonTag = tag("h4")
  def h5: CommonTag = tag("h5")
  def h6: CommonTag = tag("h6")

  def form: StdFormTag = new StdFormTag
  def table: StdTableTag = new StdTableTag
  def tr: StdTrTag = new StdTrTag("tr")
  def td: StdTdTag = new StdTdTag("td")
  def th: StdTdTag = new StdTdTag("th")

  def fieldset: CommonTag = tag("fieldset")
  def legend: CommonTag = tag("legend")

  // -------------------- inline tags --------------------

  def a: StdATag = new StdATag
  def span: CommonTag = tag("span")
  def b: CommonTag = tag("b")
  def i: CommonTag = tag("i")
  def u: CommonTag = tag("u")
  def img: StdImgTag = new StdImgTag
  def code: CommonTag = tag("code")
  def abbr: CommonTag = tag("abbr")
  def small: CommonTag = tag("small")
  def sup: CommonTag = tag("sup")
  def sub: CommonTag = tag("sub")

  def mdash: HtmlBase = {buf ++ "&mdash;"; this}
  def nbspMdash: HtmlBase = {buf ++ "&nbsp;&mdash;"; this}
  def hellip: HtmlBase = {buf ++ "&hellip;"; this}
  def laquo: HtmlBase = {buf ++ "&laquo;"; this}
  def raquo: HtmlBase = {buf ++ "&raquo;"; this}

  def label: StdLabelTag = new StdLabelTag

  def input: StdInputTag = new StdInputTag
  def inputText: StdInputTag = input.tpe("text")
  def inputEmail: StdInputTag = input.tpe("email")
  def inputNumber: StdInputNumberTag = new StdInputNumberTag().tpe("number")
  def inputTel: StdInputTag = input.tpe("tel")
  /** На андроиде такой инпут вызывает нативный контрол выбора даты */
  def inputDate: StdInputTag = input.tpe("date")
  def inputPassword: StdInputTag = input.tpe("password")
  def inputHidden: StdInputTag = input.tpe("hidden")
  def inputButton: StdInputTag = input.tpe("button")
  def inputSubmit: StdInputTag = input.tpe("submit")
  def inputFile: StdInputFileTag = new StdInputFileTag().tpe("file")
  def inputCheckbox: StdInputCheckedTag = new StdInputCheckedTag().tpe("checkbox")
  def inputRadio: StdInputCheckedTag = new StdInputCheckedTag().tpe("radio")

  def textarea: StdTextareaTag = new StdTextareaTag

  def select: StdSelectTag = new StdSelectTag
  def option: StdOptionTag = new StdOptionTag

  def button: StdButtonTag = new StdButtonTag
  def buttonButton: StdButtonTag = button.tpe("button")
  def buttonSubmit: StdButtonTag = button.tpe("submit")
  def buttonReset: StdButtonTag = button.tpe("reset")

  def font: StdFontTag = new StdFontTag

  // -------------------- misc --------------------

  def commented(name: String = null)(body: => Any): HtmlBase = {
    buf ++ "<!--"
    if (name != null) buf ++ name ++ " "
    body
    buf ++ "-->"
    this
  }
}

/**
  * Базовый класс для всех шаблонов.
  * Именно от него следует наследоваться в шаблонах, и его следует запрашивать в implicit view: HtmlView для
  * отрисовки элементов.
  * Сами же теги используют HtmlBase, и возвращают его же.
  *
  * Вся эта мешанина с двумя классами нужна только для того, чтобы отслеживать такие ошибки:
  * Есть у нас класс с методом:
  * {{{
  * class F {
  *  def h2(implicit view: HtmlView): CommonTag = view.h2.cls("form")
  * }
  * }}}
  * И он используется в шаблоне:
  * {{{
  * new HtmlView {
  *  val f = new F
  *  f.h2 {  // <- вот здесь будет ошибка, т.к., требуется написать так: "f.h2 < {", и эту ошибку уже будет подсвечивать idea.
  *    div ~ "example"
  *  }
  * }
  * }}}
  * Т.е., имея разные классы, мы можем явно показывать где использовать просто "div { ... }", а где "f.h2 < { ... }".
  * В идеале, конечно, хочется избавиться от оператора "<", но пока это неизвестно науке.
  */
abstract class StdHtmlView(buf: HtmlBuffer = new HtmlBuffer) extends HtmlBase(buf) {self =>
  implicit protected override def _self: this.type = this

  override def toString: String = result
  def result: String = buf.result
  def resultOk: PlainResult = Results.Ok.html(result)
}
