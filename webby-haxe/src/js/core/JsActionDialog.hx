package js.core;

import js.ui.dialog.Dialog;
import js.core.JsAction;
import js.ui.dialog.DialogConfig;

class JsActionDialog {
  public var dialogConfig: DialogConfig;

  public function new(dialogConfig: DialogConfig) {
    this.dialogConfig = dialogConfig;
  }

  public function defaultProcessResult(result: External) {
    JsAction.use(result, 'dialog', doDialog);
  }

  function doDialog(obj: External) {
    // Показать диалоговое окно, используя данные, пришедшие от сервера
    if (dialogConfig == null) throw new Error("No dialogConfig set");
    var bodyTag = Tag.fromHtml(obj.get('body'));
    var dlg = new Dialog(dialogConfig, bodyTag);
    dlg.onHideRedirect = obj.get('onHideRedirect');
    dlg.show();
  }
}
