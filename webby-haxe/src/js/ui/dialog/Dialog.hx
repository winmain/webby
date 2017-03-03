package js.ui.dialog;

import goog.events.EventTarget;
import js.html.MouseEvent;

@:allow(js.ui.dialog.Dialogs)
class Dialog extends EventTarget  {
  public var config(default, null): DialogConfig;

  private var isShown = false;

  /* Контейнер с серым фоном, в котором лежит само окно */
  public var containerT: Tag;

  public var tag(default, null): Tag;

  /* Этот диалог должен закрываться по нажатию ESC */
  public var escapeClose: Bool;

  /* Этот диалог в мобильной версии должен открываться на полный экран  */
//  public var mobileFullScreen: Bool = false;

  /* Если указан, то в мобильной версии этот диалог будет открываться из этого элемента. Как-бы разворачиваясь из него. */
//  public var mobileOpenFrom: Tag;

  /* Если указан, то при закрытии окна должен происходить редирект на этот урл. */
  public var onHideRedirect: String;

  /* Callback, вызываемый при клике на серый фон (container). Если вернёт true, то окно следует закрыть. */
  public var containerCloses: Void -> Bool = function() {return true;};

  public function new(config: DialogConfig, tag: Tag) {
    super();
    this.config = config;
    this.tag = tag;
    this.escapeClose = config.escapeClose;
  }

  public function show(): Void {
    if (isShown) return;

    for (fn in config.onBeforeShow) fn(this);

    Dialogs.add(this);

    config.updateClose(this);
    tag.cls(config.dialogCss);
//    if (isMobileFullscreen()) {
//      if (animate) {
//        // Анимация появления окна из открывающего блока mobileOpenFrom
//        var y: Float = mobileOpenFrom != null
//        ? mobileOpenFrom.el.clientTop - Dialogs.bodyScrollTop
//        : G.window.innerHeight;
//        t.el.style.transform = "translateY(${y}px)";
//      }
//      t.addTo(T.getBody());
//      t.cls(VisibleCss);
//      t.el.style.opacity; // Пустое получение opacity нужно, чтобы дождаться, когда браузер отрендерит новый вставленный элемент и анимация появления не будет урезана.
//      t.el.style.transform = "translateY(0)";
//      // if rr.browser.veryOldWebkit then $(window).scrollTop(0) # Для очень старого вебкита окна имеют position:absolute, и здесь нужно их прокручивать
//    } else {
    ////////////////////////////
    containerT =
    Tag.div.cls(config.containerCss).add(tag).addTo(Tag.getBody())
    .onClick(function(e: MouseEvent) {
      if (e.target != null && e.target == containerT.el) {
        if (containerCloses()) hide();
      }
    });

    G.window.setTimeout(function() {
      containerT.cls(config.containerVisibleCss);
    }, 1);

    isShown = true;
  }

  public function hide(noRedirect: Bool = false): Void {
    if (!isShown) return;

    // if rr.window.Modal.windows.count() == 1 && rr.history.whenOff('window') then return
    Dialogs.remove(this);

    if (!noRedirect && onHideRedirect != null) {
      // Сделаем редирект, но окно скрывать не будем. Лучше дождаться редиректа с открытым окном, чтобы окно закрывало собой функциональность сайта.
      config.onHideRedirect(onHideRedirect);

    } else {
      // Скрываем окно, если нет редиректов
//      if (isMobileFullscreen()) {
//        t.el.style.transform = "translateY(${G.window.innerHeight}px)";
//        G.window.setTimeout(function() {
//          if (removeOnHide) t.remove();
//          else t.clsOff(VisibleCss);
//        }, RemoveAfterTransformTime);
//      } else {
        // el.fadeOut(fadeSpeed, -> $(@).removeClass('shade').remove())

      containerT.clsOff(config.containerVisibleCss);
      G.window.setTimeout(function() {
        containerT.remove();
      }, config.removeAfterTransformTime);
    }
    isShown = false;
  }

  // ------------------------------- Private & protected methods -------------------------------

//  private function isMobileFullscreen(): Bool return mobileFullScreen && WindowSize.mobile;

  function setShade(shade: Bool) {
    containerT.setCls(config.containerShadeCss, shade);
  }
}
