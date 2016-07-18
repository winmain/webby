package webby.commons.text.html

import javax.annotation.Nullable

import org.apache.commons.lang3.StringEscapeUtils
import org.intellij.lang.annotations.Language
import webby.commons.io.Url


/**
  * Стандартный тег, от которого наследуется большинство классов тегов.
  *
  * @param tag        Название тега
  * @param shortClose Разрешить короткое закрытие тега, если он пустой? Т.е., при true можно закрывать так: <br/>, иначе <br></br>
  */
class CommonTag(val tag: String, val shortClose: Boolean = false)(implicit view: HtmlBase) extends BaseTag {
  buf.newTag(this)

  protected def buf = view.buf

  def attr(name: String): this.type = {buf.beginAttr(this) ++ ' ' ++ name endAttr(); this}

  def attr(name: String, @Nullable rawValue: String): this.type = {
    if (rawValue == null) this
    else {buf.beginAttr(this) ++ ' ' ++ name ++ "=\"" ++ rawValue ++ '"' endAttr(); this}
  }
  def attr(name: String, value: Int): this.type = {buf.beginAttr(this) ++ ' ' ++ name ++ "=\"" ++ value ++ '"' endAttr(); this}
  def attr(name: String, value: Long): this.type = {buf.beginAttr(this) ++ ' ' ++ name ++ "=\"" ++ value ++ '"' endAttr(); this}
  def attr(name: String, value: Boolean): this.type = if (value) attr(name) else this

  def attrIf(condition: Boolean, name: String, value: Int): this.type = if (condition) attr(name, value) else this
  def attrIf(condition: Boolean, name: String, value: String): this.type = if (condition) attr(name, value) else this

  def eattr(name: String, @Nullable safeValue: String): this.type = attr(name, escape(safeValue))

  def id(@Language(value = "HTML", prefix = "<div id='", suffix = "'") @Nullable v: String): this.type = attr("id", v)

  def cls(@Language(value = "HTML", prefix = "<div class='", suffix = "'") @Nullable v: String): this.type = {
    if (v != null) buf.addClass(this, v)
    this
  }
  def clsIf(condition: Boolean, @Language(value = "HTML", prefix = "<div class='", suffix = "'") @Nullable v: String): this.type =
    if (condition) cls(v) else this

  def style(@Language(value = "CSS", prefix = "* {", suffix = "}") @Nullable v: String): this.type = attr("style", v)
  def title(@Nullable raw: String): this.type = attr("title", raw)
  def titleSafe(@Nullable safe: String): this.type = title(escape(safe))

  // javascript mapping tags
  def dataToggle(@Nullable v: String): this.type = attr("data-toggle", v)
  def dataTarget(@Nullable v: String): this.type = attr("data-target", v)
  def dataType(@Nullable v: String): this.type = attr("data-type", v)
  def dataUrl(@Nullable v: String): this.type = attr("data-url", v)
  def dataOpen(@Nullable v: String): this.type = attr("data-open", v)

  // javascript shortcuts
  def onclick(@Language("JavaScript") @Nullable v: String): this.type = attr("onclick", v)
  def onchange(@Language("JavaScript") @Nullable v: String): this.type = attr("onchange", v)
  def onfocus(@Language("JavaScript") @Nullable v: String): this.type = attr("onfocus", v)

  // microdata methods
  def itemScope: this.type = attr("itemscope")
  def itemType(@Nullable v: String): this.type = attr("itemtype", v)
  def itemProp(@Nullable v: String): this.type = attr("itemprop", v)
  def itemPropIf(condition: Boolean, @Nullable v: String): this.type = if (condition) attr("itemprop", v) else this

  def apply(body: => Any): HtmlBase = {>; body; /}
  def <(body: => Any): HtmlBase = apply(body)

  protected def >
  : Unit = buf.goInTag(this)

  protected def / : HtmlBase = {
    buf.closeTag(this)
    view
  }

  def ~(rawBody: String): HtmlBase = {>; buf ++ rawBody; /}
  def ~(intBody: Int): HtmlBase = {>; buf ++ intBody; /}
  def ~(longBody: Long): HtmlBase = {>; buf ++ longBody; /}
  def ~(rawBodyOption: Option[String]): HtmlBase = rawBodyOption match {
    case Some(s) => >; buf ++ s; /
    case _ => /
  }

  def %(unsafeBody: String): HtmlBase = this.~(escape(unsafeBody))

  def %(unsafeBodyOption: Option[String]): HtmlBase = unsafeBodyOption match {
    case Some(s) => this.~(escape(s))
    case _ => /
  }

  def +(v: String): HtmlBase = {view ++ v}
  def +(v: Option[String]): HtmlBase = {v.foreach(view ++ _); view}
  def +(v: Int): HtmlBase = {view ++ v}
  def +(v: Long): HtmlBase = {view ++ v}
  def +(v: HtmlBase): HtmlBase = view
  def +(v: BaseTag): HtmlBase = view
}

// ------------------------- Tag traits -------------------------

sealed trait BaseTag {
  def tag: String
  def shortClose: Boolean

  def attr(name: String): this.type
  def attr(name: String, @Nullable value: String): this.type
  def attr(name: String, value: Int): this.type
  /** Escaped attr value */
  def eattr(name: String, @Nullable value: String): this.type

  protected def escape(s: String): String = StringEscapeUtils.escapeXml10(s)
}

sealed trait SizedTag extends BaseTag {
  def w(px: Int): this.type = attr("width", px)
  def w(v: String): this.type = attr("width", v)
  def h(px: Int): this.type = attr("height", px)
  def h(v: String): this.type = attr("height", v)
}

sealed trait ValuedTag extends BaseTag {
  def value(raw: String): this.type = attr("value", raw)
  def value(intValue: Int): this.type = attr("value", intValue)
  def value(rawOption: Option[String]): this.type = rawOption match {
    case Some(raw) => value(raw)
    case None => this
  }
  def valueSafe(safe: String): this.type = value(escape(safe))
  def valueSafe(safeOption: Option[String]): this.type = safeOption match {
    case Some(safe) => valueSafe(safe)
    case None => this
  }
}

sealed trait NamedTag extends BaseTag {
  def name(v: String): this.type = attr("name", v)
}

sealed trait DisableTag extends BaseTag {
  def disabled(v: Boolean): this.type = if (v) attr("disabled", "1") else this
  def disabled: this.type = attr("disabled", "1")
}

sealed trait AlignedTag extends BaseTag {
  def align(v: String): this.type = attr("align", v)
  def valign(v: String): this.type = attr("valign", v)
}

sealed trait MinMaxTag extends BaseTag {
  def min(v: Int): this.type = attr("min", v)
  def max(v: Int): this.type = attr("max", v)
}

sealed trait PlaceholderTag extends BaseTag {
  def placeholder(raw: String): this.type = attr("placeholder", raw)
  def placeholderSafe(safe: String): this.type = placeholder(escape(safe))
}

// ------------------------- Tag implementations -------------------------

class StdATag(implicit view: HtmlBase) extends CommonTag("a") {
  def hrefRaw(raw: String): this.type = attr("href", raw)
  def href(url: Url): this.type = hrefRaw(url.quotedUrl)
  def hrefWithAnchor(url: Url, anchor: String): this.type = hrefRaw(url.quotedUrl + "#" + anchor)
  def hrefAnchor: this.type = hrefRaw("#")
  def hrefAnchor(anchor: String): this.type = hrefRaw("#" + anchor)
  /** Специальный урл-заглушка */
  def hrefTODO: this.type = hrefRaw("#TODO")
  def hrefEmail(email: String): this.type = hrefRaw("mailto:" + email)

  def name(v: String): this.type = attr("name", v)

  def target(v: String): this.type = attr("target", v)
  def targetBlank: this.type = target("_blank")
  def targetParent: this.type = target("_parent")
  def targetSelf: this.type = target("_self")
  def targetTop: this.type = target("_top")

  def rel(v: String): this.type = attr("rel", v)
  def relNoFollow: this.type = rel("nofollow")

  def clsShowNext: this.type = cls("show-next")
  def clsShowPrev: this.type = cls("show-prev")
}

class StdScriptTag(implicit view: HtmlBase) extends CommonTag("script", shortClose = false) {
  def src(url: Url): this.type = attr("src", url.quotedUrl)
  def srcRaw(raw: String): this.type = attr("src", raw)
  def tpe(v: String): this.type = attr("type", v)
  def async: this.type = attr("async")
  def js: this.type = tpe("text/javascript")
  def js(@Language("JavaScript") raw: String): HtmlBase = js ~ raw
  def jsOnReady(body: => Unit): HtmlBase = {
    >
    buf ++ "$(function(){"
    body
    buf ++ "})"
    /
  }
}

class StdImgTag(implicit view: HtmlBase) extends CommonTag("img", shortClose = true) with SizedTag {
  def src(url: Url): this.type = attr("src", url.quotedUrl)
  def srcWithProtocol(url: Url, protocol: String): this.type = attr("src", url.quotedUrlWithProtocol(protocol))
  def srcRaw(raw: String): this.type = attr("src", raw)
  def altRaw(raw: String): this.type = attr("alt", raw)
  def border(v: Int): this.type = attr("border", v)
}

class StdFormTag(implicit view: HtmlBase) extends CommonTag("form") {
  def actionRaw(raw: String): this.type = attr("action", raw)
  def action(url: Url): this.type = actionRaw(url.quotedUrl)
  def actionIf(condition: Boolean, url: Url): this.type = if (condition) actionRaw(url.quotedUrl) else this
  def method(v: String): this.type = attr("method", v)
  def methodGet: this.type = method("get")
  def methodPost: this.type = method("post")
  def target(id: String): this.type = attr("target", id)

  // javascript shortcuts
  def onsubmit(@Language("JavaScript") v: String): this.type = attr("onsubmit", v)
}

class StdInputTag()(implicit view: HtmlBase) extends CommonTag("input", shortClose = true) with ValuedTag with DisableTag with PlaceholderTag {
  def tpe(v: String): this.type = attr("type", v)
  def name(v: String): this.type = attr("name", v)
  def maxLength(v: Int): this.type = attr("maxlength", v)

  /** Раньше значение этого атрибута было "false" в силу политики разработчиков Chrome
    * Однако тестирование 12 мая 2016г. показало, что следующие браузеры отключают автокомплит для off и не выключают для false:
    * - Firefox 46
    * - Chrome 50
    * - Android Browser 4.1
    * - Android Browser 4.2
    * - Android Browser 4.3
    * - Android Browser 4.5
    * - Android Browser 5.1
    *
    * Следующие браузеры корректно ведут себя при off и false (но похоже, что они вообще не поддерживают атрибут autocomplete):
    * - Opera 12
    * - Safari 5.0
    *
    * Ссылка, которую разработчики Chromium дают в тикете на эту проблему:
    * https://html.spec.whatwg.org/multipage/forms.html#autofill (вкратце: надо использовать off)
    */
  def autocompleteOff: this.type = attr("autocomplete", "off")
}

class StdInputCheckedTag(implicit view: HtmlBase) extends StdInputTag {
  def checked(v: Boolean): this.type = if (v) attr("checked", "1") else this
  def checked: this.type = attr("checked", "1")
}
class StdInputNumberTag(implicit view: HtmlBase) extends StdInputTag with MinMaxTag

class StdInputFileTag(implicit view: HtmlBase) extends StdInputTag {
  def accept(v: String): this.type = attr("accept", v)
}

class StdTextareaTag(implicit view: HtmlBase) extends CommonTag("textarea") with NamedTag with DisableTag with PlaceholderTag {
  def rows(v: Int): this.type = attr("rows", v)
  def cols(v: Int): this.type = attr("cols", v)
}

class StdSelectTag(implicit view: HtmlBase) extends CommonTag("select") with NamedTag with DisableTag

class StdOptionTag(implicit view: HtmlBase) extends CommonTag("option", shortClose = true) with ValuedTag with DisableTag {
  def selected(v: Boolean): this.type = if (v) attr("selected", "1") else this
  def selected: this.type = attr("selected", "1")
}

class StdButtonTag(implicit view: HtmlBase) extends CommonTag("button", shortClose = true) with NamedTag with DisableTag {
  def tpe(v: String): this.type = attr("type", v)
}

class StdLabelTag(implicit view: HtmlBase) extends CommonTag("label") {
  def forId(@Language(value = "HTML", prefix = "<label for='", suffix = "'>") v: String): this.type = attr("for", v)
}

class StdTableTag(implicit view: HtmlBase) extends CommonTag("table") with SizedTag {
  def cellpadding(v: Int): this.type = attr("cellpadding", v)
  def cellspacing(v: Int): this.type = attr("cellspacing", v)
  def border(v: Int): this.type = attr("border", v)
  def bgcolor(v: String): this.type = attr("bgcolor", v)
  def background(v: String): this.type = attr("background", v)
  def align(v: String): this.type = attr("align", v)
}

class StdTrTag(tag: String)(implicit view: HtmlBase) extends CommonTag(tag) with SizedTag with AlignedTag {
}

class StdTdTag(tag: String)(implicit view: HtmlBase) extends CommonTag(tag) with SizedTag with AlignedTag {
  def colspan(v: Int): this.type = attr("colspan", v)
  def rowspan(v: Int): this.type = attr("rowspan", v)
}

class StdFontTag(implicit view: HtmlBase) extends CommonTag("font") {
  def face(v: String): this.type = attr("face", v)
  def size(v: Int): this.type = attr("size", v)
  def color(v: String): this.type = attr("color", v)
}

class StdHrTag(implicit view: HtmlBase) extends CommonTag("hr", shortClose = true) {
  def size(v: Int): this.type = attr("size", v)
  def color(v: String): this.type = attr("color", v)
}

class StdMetaTag(implicit view: HtmlBase) extends CommonTag("meta", shortClose = true) {
  def name(rawName: String): this.type = attr("name", rawName)
  def content(rawContent: String): this.type = attr("content", rawContent)
}
