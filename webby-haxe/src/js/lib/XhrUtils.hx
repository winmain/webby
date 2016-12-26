package js.lib;

import haxe.Constraints.Function;
import js.html.XMLHttpRequest;

class XhrUtils {
  public static function bind(xhr: XMLHttpRequest, onSuccess: Function, ?onFail: Function): Void {
    xhr.onload = function() {
      if (xhr.status == 200) {
        onSuccess();
      } else if (untyped onFail) {
        onFail();
      }
    }
    if (untyped onFail) {
      xhr.onerror = onFail;
    }
  }
}
