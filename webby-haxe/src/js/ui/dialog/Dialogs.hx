package js.ui.dialog;

import js.html.KeyboardEvent;
import js.lib.ArrayUtils;

using js.lib.ArrayUtils;

/*
  Управление всеми открытыми окнами
 */
class Dialogs {
  private static var list: Array<Dialog> = [];
  private static var firstWindowShown = false;

  public static function add(dlg: Dialog): Void {
    if (!firstWindowShown) {
      initEscape();
      //rr.history.setHandlers('window', removeAll)
      //$('body').removeClass('aspage')
      firstWindowShown = true;
      if (list.nonEmpty()) {
        shadeLast(true);
      } else {
        // TODO // if rr.windowSize.mobile then ret.bodyScrollTop = $(window).scrollTop()
//        dlg.showBackdrop();
        // add window.history record
        //rr.history.push('window')
      }
    }

    list.push(dlg);
  }

  public static function remove(dlg: Dialog): Void {
    var idx = list.indexOf(dlg);
    if (idx > -1) {
      list.splice(idx, 1);
    }
    if (list.isEmpty()) {
//      dlg.hideBackdrop();
// TODO      if rr.windowSize.mobile
//             setTimeout((-> $(window).scrollTop(ret.bodyScrollTop)), 0)
    }
    else if (idx == list.length) {
      shadeLast(false);
    }
  }

  public static function removeAllExceptLast(): Void {
    if (list.length > 1) {
      for (i in 1...list.length) { // Вместо for раньше здесь был `while list.length > 1`, но это могло приводить к зацикливанию.
        list[0].hide(true);
      }
    }
  }

  public static function removeAll(): Void {
    //if rr.history.whenOff('window') then return
    if (list.length > 0) {
      for (i in 1...list.length) { // Вместо for раньше здесь был `while list.length > 1`, но это могло приводить к зацикливанию.
        list[0].hide();
      }
    }
  }

  public inline static function count(): Int return list.length;

  public inline static function last(): Dialog return list.last();

  // Сохранённая позиция скролла для мобильного приложения, потому что браузер его теряет при открытии окна
  // (из-за height:100% в стиле body.modal .main-wrapper)
  public static var bodyScrollTop: Int;

  // ------------------------------- Private & protected methods -------------------------------

  private static function initEscape(): Void {
    Tag.getBody().on('keyup', function(e: KeyboardEvent) {
      if (e.which == 27) {
        var dlg = last();
        if (untyped dlg && dlg.escapeClose) {
          dlg.hide();
          e.stopPropagation();
        }
      }
    });
  }

  private static function shadeLast(toggle: Bool): Void {
    last().setShade(toggle);
  }
}
