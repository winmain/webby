package js.ui.dialog;

class DialogConfig {
  public function new() {}

  // ------------------------------- Constants -------------------------------

  public var fadeSpeed = 200;

  /* Time in milliseconds to remove container tag with dialog in it from <body> after dialog hide. */
  public var removeAfterTransformTime = 100;

  // ------------------------------- Styles -------------------------------

  /* Class for dialog element. Automatically sets on all dialogs */
  public var dialogCss = 'dialog';


  public var bodyDialogCss = 'dialog-opened';

  /* Container with this class will be created and appended to <body> every time dialog shown.
  Container controls dialog visibility and shading.
   */
  public var containerCss = 'dialog-container';
  public var containerVisibleCss = 'dialog-container--visible';
  public var containerShadeCss = 'dialog-container--shade';

  // ------------------------------- Defaults -------------------------------

  public var escapeClose = true;

  // ------------------------------- Functions -------------------------------

  /* Functions, called before showing a dialog. */
  public var onBeforeShow: Array<Dialog -> Void> = [];

  public function onHideRedirect(url: String) {
    if (url == '#') G.location.reload()
    else G.location.href = url;
  }

  /*
  Find close button with class `close` and add close handler on it.
   */
  public function updateClose(dialog: Dialog) {
//    that = @
//    $((if rr.windowSize.mobile then '.close, header' else '.close'), @el).click () ->
//      that.hide()
    dialog.tag.fndAnd('.close', function(t: Tag) {t.off('click', dialog.hide).on('click', dialog.hide);});
  }

  // ------------------------------- Helpers -------------------------------

  public function create(tag: Tag): Dialog return new Dialog(this, tag);
}
