package ;
import goog.Goog;
import haxe.Constraints.Function;
import haxe.extern.EitherType;
import js.html.AnchorElement;
import js.html.BodyElement;
import js.html.ButtonElement;
import js.html.Element;
import js.html.FormElement;
import js.html.HTMLCollection;
import js.html.HtmlElement;
import js.html.InputElement;
import js.html.LabelElement;
import js.html.Node;
import js.html.NodeList;
import js.html.ObjectElement;
import js.html.OptGroupElement;
import js.html.OptionElement;
import js.html.ProgressElement;
import js.html.SelectElement;
import js.html.svg.ImageElement;
import js.html.svg.ScriptElement;
import js.html.TableElement;
import js.html.TemplateElement;
import js.html.TextAreaElement;
import js.lib.EventUtils;

/*
Html element wrapper
 */
class Tag {
  public static function tag(tag: String): Tag return new Tag(G.document.createElement(tag));
  public inline static function wrap(el: Null<Element>): Null<Tag> return el == null ? null : new Tag(el);

  /*
  Create new html element wrapped with Tag from string
  @param {String} HTML representing a single element
  */
  public static function fromHtml(html: String): Tag {
    var template: TemplateElement = cast G.document.createElement('template');
    template.innerHTML = html;
    return wrap(template.content.firstElementChild);
  }

  @:keep
  @:expose('Tag.find')
  public static function find(selectors: String): Null<Tag> return find2(G.document, selectors);

  public static function findAll(selectors: String): Array<Tag> return findAll2(G.document, selectors);

  public static function findAnd(selectors: String, action: Tag -> Void): Void return findAnd2(G.document, selectors, action);

  public static function findById(id: String): Null<Tag> return wrap(G.document.getElementById(id));

  public static function findByCls(cls: String): Null<Tag> return findByCls2(G.document, cls);

  public static function findAllByCls(cls: String): Array<Tag> return findAllByCls2(G.document, cls);


  private static function find2(base: {function querySelector(s: String): Element;}, selectors: String): Null<Tag> {
    var el = base.querySelector(selectors);
    return untyped el ? wrap(el) : null;
  }

  private static function findAll2(base: {function querySelectorAll(s: String): NodeList;}, selectors: String): Array<Tag> {
    var nodes = base.querySelectorAll(selectors);
    var result: Array<Tag> = [];
    for (node_ in nodes) {
      var node: Node = node_;
      if (G.instanceof(node, Element)) result.push(wrap(cast node));
    }
    return result;
  }

  private static function findAnd2(base: {function querySelectorAll(s: String): NodeList;}, selectors: String, action: Tag -> Void) {
    for (tag in findAll2(base, selectors)) {
      action(tag);
    }
  }

  private static function findByCls2(base: {function getElementsByClassName(s: String): HTMLCollection;}, cls: String): Null<Tag> {
    var found = base.getElementsByClassName(cls);
    return found.length == 0 ? null : wrap(found[0]);
  }

  private static function findAllByCls2(base: {function getElementsByClassName(s: String): HTMLCollection;}, cls: String): Array<Tag> {
    var found = base.getElementsByClassName(cls);
    var result: Array<Tag> = [];
    var idx = 0, len = found.length;
    while (idx < len) {
      result.push(wrap(found[idx]));
      idx++;
    }
    return result;
  }

  public static function getBody(): Tag return wrap(G.document.body);
  public static function getDocument(): Tag return wrap(G.document.documentElement);

  // ------------------------------- Block elements -------------------------------

  public static var div(get, never): Tag;
  public static function get_div(): Tag return tag("div");

  public static var br(get, never): Tag;
  public static function get_br(): Tag return tag("br");

  public static var p(get, never): Tag;
  public static function get_p(): Tag return tag("p");

  public static var h1(get, never): Tag;
  public static function get_h1(): Tag return tag("h1");

  public static var h2(get, never): Tag;
  public static function get_h2(): Tag return tag("h2");

  public static var h3(get, never): Tag;
  public static function get_h3(): Tag return tag("h3");

  public static var h4(get, never): Tag;
  public static function get_h4(): Tag return tag("h4");

  public static var h5(get, never): Tag;
  public static function get_h5(): Tag return tag("h5");

  public static var h6(get, never): Tag;
  public static function get_h6(): Tag return tag("h6");

  public static var form(get, never): Tag;
  public static function get_form(): Tag return tag("form");

  public static var table(get, never): Tag;
  public static function get_table(): Tag return tag("table");

  public static var tr(get, never): Tag;
  public static function get_tr(): Tag return tag("tr");

  public static var td(get, never): Tag;
  public static function get_td(): Tag return tag("td");

  public static var th(get, never): Tag;
  public static function get_th(): Tag return tag("th");

  public static var section(get, never): Tag;
  public static function get_section(): Tag return tag("section");

  // ------------------------------- Inline elements -------------------------------

  public static var span(get, never): Tag;
  public static function get_span(): Tag return tag("span");

  public static var a(get, never): Tag;
  public static function get_a(): Tag return tag("a");

  public static var b(get, never): Tag;
  public static function get_b(): Tag return tag("b");

  public static var i(get, never): Tag;
  public static function get_i(): Tag return tag("i");

  public static var u(get, never): Tag;
  public static function get_u(): Tag return tag("u");

  public static var img(get, never): Tag;
  public static function get_img(): Tag return tag("img");

  public static var small(get, never): Tag;
  public static function get_small(): Tag return tag("small");

  public static var label(get, never): Tag;
  public static function get_label(): Tag return tag("label");

  public static function labelFor(id: String): Tag return label.attr('for', id);

  public static var input(get, never): Tag;
  public static function get_input(): Tag return tag("input");

  public static var textarea(get, never): Tag;
  public static function get_textarea(): Tag return tag("textarea");

  public static var select(get, never): Tag;
  public static function get_select(): Tag return tag("select");

  public static var option(get, never): Tag;
  public static function get_option(): Tag return tag("option");

  public static var button(get, never): Tag;
  public static function get_button(): Tag return tag("button");

  // ------------------------------- Helper strings -------------------------------

  public inline static var nbsp = "\u00A0";
  public inline static var mdash = "—";
  public inline static var laquo = "«";
  public inline static var raquo = "»";

  // ------------------------------- Class Tag -------------------------------

  public var el(default, null): Element;

  public function new(el: Element) {
    this.el = el;
  }

  // ------------------------------- Element getters -------------------------------

  inline public function anchorElement(): AnchorElement return cast el;

  inline public function bodyElement(): BodyElement return cast el;

  inline public function buttonElement(): ButtonElement return cast el;

  inline public function formElement(): FormElement return cast el;

  inline public function htmlElement(): HtmlElement return cast el;

  inline public function imageElement(): ImageElement return cast el;

  inline public function inputElement(): InputElement return cast el;

  inline public function labelElement(): LabelElement return cast el;

  inline public function objectElement(): ObjectElement return cast el;

  inline public function optGroupElement(): OptGroupElement return cast el;

  inline public function optionElement(): OptionElement return cast el;

  inline public function progressElement(): ProgressElement return cast el;

  inline public function scriptElement(): ScriptElement return cast el;

  inline public function selectElement(): SelectElement return cast el;

  inline public function tableElement(): TableElement return cast el;

  inline public function textAreaElement(): TextAreaElement return cast el;

  // ------------------------------- Getters -------------------------------

  public function getId(): String return el.id;

  public function hasCls(v: String): Bool return el.classList.contains(v);

  public function getAttr(key: String): String return el.getAttribute(key);

  public function hasAttr(key: String): Bool return el.hasAttribute(key);

  public function getVal(): String {
    // TODO: нужны обработчики для других типов элементов типа radio, checkbox, textarea, select
    return untyped el.value;
  }

  public function getHtml(): String return el.innerHTML;

  public function getType(): String return untyped el.type;

  public function getHref(): String return untyped el.href;

  // ------------------------------- Setters -------------------------------

  public function id(v: String): Tag {
    el.id = v;
    return this;
  }


  public function cls(v: String): Tag {
    if (v.indexOf(' ') == -1) {
      el.classList.add(v);
    } else {
      for (clazz in v.split(' ')) el.classList.add(clazz);
    }
    return this;
  }

  public function clsOff(v: String): Tag return setCls(v, false);

  public function clsIf(cond: Bool, v: String): Tag return if (cond) cls(v) else this;

  public function setCls(token: String, v: Bool): Tag {
    el.classList.toggle(token, v);
    return this;
  }


  public function attr(key: String, value: Dynamic): Tag {
    if (Goog.isBoolean(value)) {
      if (untyped value) el.setAttribute(key, '1');
      else el.removeAttribute(key);
    } else {
      el.setAttribute(key, value);
    }
    return this;
  }

  public function removeAttr(key: String): Tag return attr(key, false);


  public function setVal(v: Dynamic): Tag {
    // TODO: нужны обработчики для других типов элементов типа radio, checkbox, textarea, select
    untyped el.value = v;
    return this;
  }


  public function setHtml(html: String): Tag {
    el.innerHTML = html;
    return this;
  }

  /*
  Add html code to element
   */
  public function addHtml(v: String): Tag {
    var div = G.document.createElement('div');
    div.innerHTML = v;
    while (div.childNodes.length > 0) {
      el.appendChild(div.childNodes.item(0));
    }
    return this;
  }


  public function style(v: String): Tag {
    untyped el.style.cssText = v; // cssText for Safari
    return this;
  }

  public function type(v: String): Tag {
    untyped el.type = v;
    return this;
  }

  public function href(v: String): Tag {
    untyped el.href = v;
    return this;
  }

  // ------------------------------- Element manipulation -------------------------------

  /*
  Add child to element
   */
  public function add(v: EitherType<String, EitherType<Tag, Node>>): Tag {
    if (Goog.isString(v)) {
      el.appendChild(G.document.createTextNode(v));
    } else if (untyped v.el) {
      el.appendChild((v: Tag).el);
    } else {
      el.appendChild(v);
    }
    return this;
  }

  /*
  Add nullable child to element.
  If this child is null simple ignore it.
   */
  public function add2(v: Null<EitherType<String, EitherType<Tag, Node>>>): Tag {
    return v == null ? this : add(v);
  }

  public function addTo(tag: EitherType<Tag, Node>): Tag {
    if (untyped tag.el) {
      (tag: Tag).el.appendChild(el);
    } else {
      (tag: Node).appendChild(el);
    }
    return this;
  }

  /*
  Add this tag after specified #tag.
   */
  public function addAfter(tag: EitherType<Tag, Node>): Tag {
    var node: Node = (untyped tag.el) ? (tag: Tag).el : (tag: Node);
    node.parentNode.insertBefore(el, node.nextSibling);
    return this;
  }

  public function remove(): Tag {
    el.parentElement.removeChild(el);
    return this;
  }

  public function removeChildren(): Tag {
    while (el.hasChildNodes()) {
      el.removeChild(el.lastChild);
    }
    return this;
  }

  // ------------------------------- Visibility -------------------------------

  public function visible(): Bool return untyped __strict_neq__(el.offsetParent, null);

  public function show(displayValue: String = 'block', v: Bool = true): Tag {
    var vis = visible();
    if (v && !vis) {
      // Показать элемент
      el.style.display = displayValue;
    } else if (!v && vis) {
      // Скрыть элемент
      el.style.display = 'none';
    }
    return this;
  }

  public function hide(): Tag return show('', false);

  public function showToggle(displayValue: String = 'block'): Tag return show(displayValue, !visible());

  // ------------------------------- Traverse -------------------------------

  public function iterator(): Iterator<Tag> return new TagIterator(el.children);

  public function fnd(selectors: String): Null<Tag> return find2(el, selectors);

  public function fndAll(selectors: String): Array<Tag> return findAll2(el, selectors);

  public function fndAnd(selectors: String, action: Tag -> Void): Void return findAnd2(el, selectors, action);

  public function fndByCls(cls: String): Null<Tag> return findByCls2(el, cls);

  public function fndAllByCls(cls: String): Array<Tag> return findAllByCls2(el, cls);

  /*
  Find first parent which satisfy `selector`.
   */
  public function fndParent(selector: Tag -> Bool): Null<Tag> {
    var par = parent();
    while (par != null) {
      if (selector(par)) return par;
      par = par.parent();
    }
    return null;
  }

  public function parent(): Null<Tag> return wrap(el.parentElement);

  public function prev(): Null<Tag> return wrap(el.previousElementSibling);

  public function next(): Null<Tag> return wrap(el.nextElementSibling);

  // ------------------------------- Misc -------------------------------

  public function equals(other: Tag): Bool return el == other.el;

  public function clone(?deep: Bool): Tag return wrap(cast el.cloneNode(deep));

  // force reflow, see http://matheusazzi.com/animating-from-display-none-with-css-and-callbacks/
  public function forceReflow(): Bool return el.offsetWidth >= 0;

  // ------------------------------- Events -------------------------------

  public function on(eventName: String, handler: Function, ?options: Dynamic): Tag {
    el.addEventListener(eventName, handler, options);
    return this;
  }

  public function onMulti(eventNames: Array<String>, handler: Function, ?options: Dynamic): Tag {
    for (name in eventNames) {
      el.addEventListener(name, handler, options);
    }
    return this;
  }

  public function off(eventName: String, handler: Function, ?options: Dynamic): Tag {
    el.removeEventListener(eventName, handler, options);
    return this;
  }

  public function offMulti(eventNames: Array<String>, handler: Function, ?options: Dynamic): Tag {
    for (eventName in eventNames) {
      el.removeEventListener(eventName, handler, options);
    }
    return this;
  }

  public function trigger(eventName: String): Tag {
    EventUtils.fireEvent(el, eventName);
    return this;
  }

  // MouseEvent
  public function onClick(handler: Function): Tag return on('click', untyped handler);
}


class TagIterator {
  private var i = 0;
  private var children: Dynamic;

  public function new(children: Dynamic) {
    this.children = children;
  }

  public function hasNext(): Bool return i < children.length;

  public function next(): Tag return Tag.wrap(children[i++]);
}
