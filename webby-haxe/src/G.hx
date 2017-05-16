package ;
import js.RegExp;
import js.html.Storage;
import js.html.Window;
import js.html.XMLHttpRequest;

/**
 * Глобальный класс, содержит шоткаты на часто используемые классы и методы.
 **/
class G {

  // ------------------------------- Language helpers & js hacks -------------------------------

  inline public static function identity<T>(s: T): T return s;

  inline public static function instanceof(value: Dynamic, cls: Class<Dynamic>): Bool return untyped __js__('{0} instanceof {1}', value, cls);

  inline public static function toString(f: Dynamic): String return untyped __js__('""+{0}', f);

  inline public static function toInt(s: Dynamic): Int return untyped __js__('(+({0}))', s);

  inline public static function toFloat(s: Dynamic): Float return untyped __js__('(+({0}))', s);

  inline public static function toBool(any: Dynamic): Bool return untyped __js__('!!({0})', any);

  inline public static function regexp(s: String): RegExp return new RegExp(s);

  inline public static function floatToFixed(f: Float, numbers: Int): String return untyped __js__('{0}.toFixed({1})', f, numbers);

  inline public static function or<T>(a: T, b: Void -> T): T return untyped __js__('({0} || {1})', a, b());

  inline public static function and<T>(a: T, b: Void -> T): T return untyped __js__('({0} && {1})', a, b());

  inline public static function and2<A, T>(a: A, b: A -> T): T return untyped __js__('({0} && {1})', a, b(a));

  public static function require(v: Bool, error: String = null): Void {
    if (!v) throw (error == null ? "Requirement failed" : error);
  }

  // ------------------------------- Browser methods -------------------------------

  /** The global window object. */
  public static var window(get, never): js.html.Window;

  inline static function get_window() return untyped __js__("window");

  inline public static function windowGet(globalVarName: String): External return untyped __js__('window[{0}]', globalVarName);

  inline public static function windowSet(globalVarName: String, value: Dynamic): External return untyped __js__('window[{0}] = {1}', globalVarName, value);

  /** Shortcut to Window.document. */
  public static var document(get, never): js.html.HTMLDocument;

  inline static function get_document() return untyped __js__("window.document");

  /** Shortcut to Window.location. */
  public static var location(get, never): js.html.Location;

  inline static function get_location() return untyped __js__("window.location");

  /** Shortcut to Window.navigator. */
  public static var navigator(get, never): js.html.Navigator;

  inline static function get_navigator() return untyped __js__("window.navigator");

  /** Shortcut to Window.console. */
  public static var console(get, never): js.html.Console;

  inline static function get_console() return untyped __js__("window.console");

  inline public static function log(data: Dynamic): Void return console.log(data);

  /**
	 * True if a window object exists, false otherwise.
	 *
	 * This can be used to check if the code is being executed in a non-browser
	 * environment such as node.js.
	 */
  public static var supported(get, never): Bool;

  inline static function get_supported() return untyped __typeof__(window) != "undefined";

  /**
	 * Safely gets the browser's local storage, or returns null if localStorage is unsupported or
	 * disabled.
	 */

  public static function getLocalStorage(): Storage {
    try {
      var s = window.localStorage;
      s.getItem("");
      return s;
    } catch (e: Dynamic) {
      return null;
    }
  }

  /**
	 * Safely gets the browser's session storage, or returns null if sessionStorage is unsupported
	 * or disabled.
	 */

  public static function getSessionStorage(): Storage {
    try {
      var s = window.sessionStorage;
      s.getItem("");
      return s;
    } catch (e: Dynamic) {
      return null;
    }
  }

  /**
	 * Creates an XMLHttpRequest, with a fallback to ActiveXObject for ancient versions of Internet
	 * Explorer.
	 */

  public static function createXMLHttpRequest(): XMLHttpRequest {
    if (untyped __js__("typeof XMLHttpRequest") != "undefined") {
      return new XMLHttpRequest();
    }
    if (untyped __js__("typeof ActiveXObject") != "undefined") {
      return untyped __new__("ActiveXObject", "Microsoft.XMLHTTP");
    }
    throw "Unable to create XMLHttpRequest object.";
  }

  /**
		Display an alert message box containing the given message. See also `Window.alert()`.
	 */

  inline public static function alert(v: Dynamic) {
    @:privateAccess window.alert(cast v);
  }

  // ------------------------------- core-script.js methods -------------------------------

  /**
   * Добавить функцию на выполнение после загрузки всех jsParts.
   * Если все jsParts загружены, то функция будет выполнена немедленно.
   */
  inline public static function jsOnLoad(exec: Void -> Void): Void return untyped __js__('jsOnLoad({0})', exec);

  /**
   * Загрузка jsPart только если он не был загружен.
   * @param name Имя части, как он записан в jsParts
   * @param url Урл для подгрузки
   */
  inline public static function jsPartLoad(name: String, url: String):Void return untyped __js__('jsPartLoad({0}, {1})', name, url);

  /**
   * Выполнить код #exec только после загрузки части #partName.
   * Если #partName уже загружен, то код будет тут же выполнен.
   * @export
   */
  inline public static function jsPartExec(partName: String, exec: Void -> Void): Void return untyped __js__('jsPartExec({0}, {1})', partName, exec);

  /**
   * Выполнить код после подгрузки _rest.js файла. Если же RestPart не задан, то код будет выполнен сразу же.
   * @param that this для execOnRest
   * @param execOnRest Код, выполняемый после подгрузки rest файла.
   * @return {boolean}
   * @export
   */
  inline public static function Rest(that: Dynamic, execOnRest: Void -> Void): Void return untyped __js__('Rest({0}, {1})', that, execOnRest);
}
