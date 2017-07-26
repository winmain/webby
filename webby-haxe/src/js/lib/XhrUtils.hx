package js.lib;

import haxe.Constraints.Function;
import haxe.Json;
import js.html.XMLHttpRequest;
import js.html.XMLHttpRequestResponseType;

class XhrUtils {
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
  Simple JSON GET request
   */
  public static function jsonGet(url: String, onSuccess: External -> Void, ?onFail: XMLHttpRequest -> Void): Void {
    var xhr = new XMLHttpRequest();
    bind(xhr, function() {
      onSuccess(xhr.response);
    }, function() {
      if (onFail != null) onFail(xhr);
    });
    xhr.open('GET', url, true);
    setJsonResponseType(xhr);
    xhr.send();
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

  /*
  Simple text GET request
   */
  public static function textGet(url: String, onSuccess: String -> Void, ?onFail: XMLHttpRequest -> Void): Void {
    var xhr = new XMLHttpRequest();
    bind(xhr, function() {
      onSuccess(xhr.responseText);
    }, function() {
      if (onFail != null) onFail(xhr);
    });
    xhr.open('GET', url, true);
    xhr.responseType = XMLHttpRequestResponseType.TEXT;
    xhr.send();
  }
}
