package js.lib;

import haxe.extern.EitherType;

class DateUtils {
  // ------------------------------- Javascript Date -------------------------------

  /*
  Empty date constructor
   */
  public static inline function newEmpty(): Date return
    untyped __js__("new Date()");

  /*
  Value or string constructor
   */
  public static inline function newValue(value: EitherType<Float, String>): Date return
    untyped __js__("new Date({0})", value);

  /*
  Common constructor
   */
  public static inline function newStd(year: Int, month: Int, ?day: Int, ?hour: Int, ?min: Int, ?sec: Int, ?ms: Int): Date return
    if (day == null) untyped __js__("new Date({0}, {1})", year, month);
    else if (hour == null) untyped __js__("new Date({0}, {1}, {2})", year, month, day);
    else if (min == null) untyped __js__("new Date({0}, {1}, {2}, {3})", year, month, day, hour);
    else if (sec == null) untyped __js__("new Date({0}, {1}, {2}, {3}, {4})", year, month, day, hour, min);
    else if (ms == null) untyped __js__("new Date({0}, {1}, {2}, {3}, {4}, {5})", year, month, day, hour, min, sec);
    else untyped __js__("new Date({0}, {1}, {2}, {3}, {4}, {5}, {6})", year, month, day, hour, min, sec, ms);

  /*
  Now in millis
   */
  public static inline function nowMillis(d: Date): Float return
    untyped __js__("{0}.now()", d);

  // ------------------------------- Built-in UTC methods -------------------------------

  public static inline function getUTCFullYear(d: Date): Int return
    untyped __js__("{0}.getUTCFullYear()", d);

  public static inline function getUTCMonth(d: Date): Int return
    untyped __js__("{0}.getUTCMonth()", d);

  public static inline function getUTCDate(d: Date): Int return
    untyped __js__("{0}.getUTCDate()", d);

  public static inline function getUTCHours(d: Date): Int return
    untyped __js__("{0}.getUTCHours()", d);

  public static inline function getUTCMinutes(d: Date): Int return
    untyped __js__("{0}.getUTCMinutes()", d);

  public static inline function getUTCSeconds(d: Date): Int return
    untyped __js__("{0}.getUTCSeconds()", d);
}
