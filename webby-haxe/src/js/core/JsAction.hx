package js.core;

class JsAction {
  public function new() {}

  /*
  Process `JsActionResult` from server.
  Must be overridden in subclasses
   */
  public function processResult(result: External, ?ctx: External) {}

  function defaultProcessResult(result: External) {
    use(result, 'redirect', doRedirect);
    use(result, 'html', doHtml);
    use(result, 'exec', doExec);
  }

  inline static public function use(result: External, key: String, handler: Dynamic -> Void) {
    var obj = result.get(key);
    if (obj) {
      handler(obj);
    }
  }

  function doRedirect(url: String) {
    if (url == '#') G.location.reload();
    else G.location.href = url;
  }

  function doHtml(obj: External) {
    var selector: String = obj.get('selector');
    var body: String = obj.get('body');
    Tag.findAnd(selector, function(t: Tag) {t.setHtml(body);});
  }

  function doExec(code: String) {
    Lib.eval(code);
  }
}
