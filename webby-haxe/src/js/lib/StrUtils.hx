package js.lib;

import haxe.extern.EitherType;

class StrUtils {
  /*
  Left pad a String with zeroes.
   */
  public static function zpad(num: EitherType<Float, String>, zeroes: Int): String {
    return G.identity('0' + num).slice(-zeroes);
  }
}
