package ;
import haxe.Json;

class JSON {
  public static inline function parse(text: String): External return Json.parse(text);

  public static inline function stringify(value: Dynamic, ?replacer: Dynamic -> Dynamic -> Dynamic, ?space: String): String return Json.stringify(value, replacer, space);
}
