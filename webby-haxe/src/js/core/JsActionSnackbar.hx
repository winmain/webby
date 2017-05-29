package js.core;

import js.mdl.MdlUtil;

class JsActionSnackbar {
  public function new() {
  }

  public function defaultProcessResult(result: External) {
    JsAction.use(result, 'snackbar', doSnackbar);
  }

  function doSnackbar(obj: External) {
    MdlUtil.showTemporarySnackbar(obj);
  }
}
