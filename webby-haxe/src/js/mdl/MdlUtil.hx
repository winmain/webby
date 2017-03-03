package js.mdl;

import js.html.Element;
import js.html.Event;
import js.ui.dialog.Dialog;
import js.ui.dialog.DialogConfig;

class MdlUtil {
  /* Пропатчить Dialog */
  public static function useDialogs(config: DialogConfig): Void {
    config.onBeforeShow.push(function(dlg: Dialog) {ComponentHandler.upgradeElements(dlg.tag.el);});
  }

  /*
  Метод помогает убрать фоновый текст после смены значения `mdl-textfield`
   */
  public static function afterTextFieldSetValue(input: Element): Void {
    input.dispatchEvent(new Event('input'));
  }

  public static function textFieldSetValue(inputT: Tag, value: Dynamic): Void {
    inputT.setVal(value);
    afterTextFieldSetValue(inputT.el);
  }

  /*
  Создать и добавить snackbar в body
   */
  public static function createAndAddSnackbar(): Tag {
    var container = Tag.div.cls("mdl-js-snackbar mdl-snackbar")
    .add(Tag.div.cls("mdl-snackbar__text"))
    .add(Tag.button.cls("mdl-snackbar__action"))
    .addTo(Tag.getBody());
    ComponentHandler.upgradeElement(container.el);
    return container;
  }

  /*
  Показать snackbar вокруг специально созданного элемента
   */
  public static function showSnackbar(container: Tag, data: ShowSnackbar): Void {
    ((container.el:External).get('MaterialSnackbar').get('showSnackbar'):ShowSnackbar -> Void)(data);
  }

  /*
  Создать, показать, и удалить snackbar
  Вернуть временный контейнер snackbar'а, чтобы можно было досрочно его закрыть
   */
  public static function showTemporarySnackbar(data: ShowSnackbar): Tag {
    var container = createAndAddSnackbar();
    G.window.setTimeout(function() { // этот setTimeout нужен, чтобы snackbar открылся с анимацией
      showSnackbar(container, data);
      var timeout: Int = (untyped (data:External).get('timeout') || 2750) + 1000;
      G.window.setTimeout(function() {container.remove();}, timeout);
    }, 1);
    return container;
  }

  /*
  Создать, показать, и удалить snackbar
  Вернуть временный контейнер snackbar'а, чтобы можно было досрочно его закрыть
   */
  public static function showTemporarySnackbar2(message: String, ?timeout: Int): Tag return showTemporarySnackbar({
    'message': message,
    'timeout': timeout
  });

  /*
  Закрыть snackbar
   */
  public static function closeSnackbar(container: Tag): Void {
    ((container.el:External).get('MaterialSnackbar').get('cleanup_'):Void -> Void)();
  }

  /*
  Закрыть левое меню
   */
  public static function closeDrawer(): Void {
    Tag.findAnd('.mdl-layout__obfuscator.is-visible', function(t: Tag) return t.el.click());
  }
}

/*
Данные для показа snackbar.
Внимание! Все поля должны быть обрамлены в кавычки, потому что это внешний интерфейс, например:
{
  "message": "qwe",
  "timeout": 5000
}
 */
typedef ShowSnackbar = {
  // The text message to display. 	Required
  var message: String;
  // The amount of time in milliseconds to show the snackbar. 	Optional (default 2750)
  @:optional var timeout: Int;
  // The function to execute when the action is clicked. 	Optional
  @:optional function actionHandler(): Void;
  // The text to display for the action button. 	Required if actionHandler is set
  @:optional var actionText: String;
}
