package js.lib;

import haxe.Constraints.Function;
import haxe.Json;
import js.html.XMLHttpRequest;
import js.html.XMLHttpRequestResponseType;

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

  public static function setJsonContentType(xhr: XMLHttpRequest): Void {
    xhr.setRequestHeader("Content-type", "application/json");
  }

  public static function setJsonResponseType(xhr: XMLHttpRequest): Void {
    xhr.responseType = XMLHttpRequestResponseType.JSON;
  }


  /*
  Simple JSON POST request
   */
  public static function jsonPost(url: String, postData: External, onSuccess: External -> Void, ?onFail: XMLHttpRequest -> Void): Void {
    var xhr = new XMLHttpRequest();
    bind(xhr, function() {
      onSuccess(xhr.response);
    }, function() {
      if (onFail != null) onFail(xhr);
    });
    xhr.open('POST', url, true);
    setJsonContentType(xhr);
    setJsonResponseType(xhr);
    xhr.send(Json.stringify(postData));
  }
}
