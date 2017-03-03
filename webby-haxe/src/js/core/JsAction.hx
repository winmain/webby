package js.core;

import js.mdl.MdlUtil;
import js.ui.dialog.Dialog;
import js.ui.dialog.DialogConfig;

class JsAction {
  public function new() {}

  public var dialogConfig: DialogConfig;

  /*
  Process `JsActionResult` from server.
  Must be overridden in subclasses
   */
  public function processResult(result: External) {}

  function defaultProcessResult(result: External) {
    use(result, 'redirect', doRedirect);
    use(result, 'dialog', doDialog);
    use(result, 'snackbar', doSnackbar);
    use(result, 'html', doHtml);
    use(result, 'exec', doExec);
  }

  inline function use(result: External, key: String, handler: Dynamic -> Void) {
    var obj = result.get(key);
    if (obj) {
      handler(obj);
    }
  }

  function doRedirect(url: String) {
    if (url == '#') G.location.reload();
    else G.location.href = url;
  }

  function doDialog(obj: External) {
    // Показать диалоговое окно, используя данные, пришедшие от сервера
    if (dialogConfig == null) throw new Error("No dialogConfig set");
    var bodyTag = Tag.fromHtml(obj.get('body'));
    var dlg = new Dialog(dialogConfig, bodyTag);
    dlg.onHideRedirect = obj.get('onHideRedirect');
    dlg.show();
  }

  function doSnackbar(obj: External) {
    MdlUtil.showTemporarySnackbar(obj);
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
