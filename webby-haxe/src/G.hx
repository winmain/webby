package ;
import haxe.extern.EitherType;
import js.Error;
import js.html.Storage;
import js.html.Window;
import js.RegExp;

/**
 * Глобальный класс, содержит шоткаты на часто используемые классы и методы.
 **/
class G {

  // ------------------------------- Language helpers & js hacks -------------------------------

  inline public static function identity<T>(s: T): T return s;

  inline public static function instanceof(value: Dynamic, cls: Class<Dynamic>): Bool return untyped __js__('{0} instanceof {1}', value, cls);

  inline public static function toString(f: Dynamic): String return untyped __js__('""+{0}', f);

  /* @see http://2ality.com/2012/03/converting-to-string.html */
  inline public static function toString2(f: Dynamic): String return untyped __js__('String({0})', f);

  inline public static function toInt(s: Dynamic): Int return untyped __js__('(+({0}))', s);

  inline public static function toFloat(s: Dynamic): Float return untyped __js__('(+({0}))', s);

  inline public static function toBool(any: Dynamic): Bool return untyped __js__('!!({0})', any);

  inline public static function asBool(any: Dynamic): Bool return any;

  inline public static function regexp(s: String): RegExp return new RegExp(s);

  inline public static function floatToFixed(f: Float, numbers: Int): String return untyped __js__('{0}.toFixed({1})', f, numbers);

  inline public static function or<A, T>(a: A, b: Void -> T): EitherType<A, T> return untyped __js__('({0} || {1})', a, b());

  inline public static function and<T>(a: Any, b: Void -> T): T return untyped __js__('({0} && {1})', a, b());

  inline public static function and2<A, T>(a: A, b: A -> T): T return untyped __js__('({0} && {1})', a, b(a));

  public static function error(message: String): Void {
    throw new Error(message);
  }

  public static function require<T>(v: T, error: String = null): T {
    if (untyped !v) throw new Error(error == null ? "Requirement failed" : error);
    return v;
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
		Display an alert message box containing the given message. See also `Window.alert()`.
	 */
  inline public static function alert(v: Dynamic) {
    @:privateAccess window.alert(cast v);
  }
}
