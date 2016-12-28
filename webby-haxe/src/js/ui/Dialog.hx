package js.ui;

import js.core.WindowSize;
import goog.events.EventTarget;
import js.html.MouseEvent;

@:allow(js.ui.Dialogs)
class Dialog extends EventTarget  {
  public static inline var FadeSpeed = 200;
  public static inline var RemoveAfterTransformTime = 100;

  public static inline var BodyDialogCss = 'dialog-opened';
  public static inline var ContainerCss = 'dialog-container';
  public static inline var ContainerVisibleCss = 'dialog-container--visible';
  public static inline var ContainerShadeCss = 'dialog-container--shade';
  public static inline var DialogCss = 'dialog';

  private var isShown = false;

  /* Контейнер с серым фоном, в котором лежит само окно */
  public var containerT: Tag;

  public var t(default, null): Tag;

  /* Этот диалог должен закрываться по нажатию ESC */
  public var escapeClose: Bool = true;

  /* Открыть этот диалог с анимацией TODO: переделать на css-анимации */
  public var animate: Bool = true;

  /* Этот диалог в мобильной версии должен открываться на полный экран  */
  public var mobileFullScreen: Bool = false;

  /* Если указан, то в мобильной версии этот диалог будет открываться из этого элемента. Как-бы разворачиваясь из него. */
  public var mobileOpenFrom: Tag;

  /* Если указан, то при закрытии окна должен происходить редирект на этот урл. */
  public var onHideRedirect: String;

  /* Callback, вызываемый при клике на серый фон (container). Если вернёт true, то окно следует закрыть. */
  public var containerCloses: Void -> Bool = function() {return true;};

  /* Колбеки, вызываемые перед показом диалога. Созданы для расширения этого класса. */
  public static var onBeforeShow: Array<Dialog -> Void> = [];

  public function new(t: Tag) {
    super();
    this.t = t;
  }

  public function show(): Void {
    if (isShown) return;

    for (fn in onBeforeShow) fn(this);

    Dialogs.add(this);

    updateClose();
    t.cls(DialogCss);
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
    Tag.div.cls(ContainerCss).add(t).addTo(Tag.getBody())
    .onClick(function(e: MouseEvent) {
      if (e.target != null && e.target == containerT.el) {
        if (containerCloses()) hide();
      }
    });

    G.window.setTimeout(function() {
      containerT.cls(ContainerVisibleCss);
    }, 1);

      // TODO: css // if !noAnim then el.fadeOut(0).fadeIn(fadeSpeed)
//    }
    isShown = true;
    // TODO: // position: absolute включается для планшетов, чтобы окно не прыгало при попытке зуминга на элемент в окне
//    if !rr.windowSize.mobile && el.css('position') == 'absolute'
//      el.css('top', $('body').scrollTop() + 60)
  }

  public function hide(noRedirect: Bool = false): Void {
    if (!isShown) return;

    // if rr.window.Modal.windows.count() == 1 && rr.history.whenOff('window') then return
    Dialogs.remove(this);
    if (!(!noRedirect && onHideRedirect != null)) { // Если указан onHideRedirect, то окно лучше не скрывать, а просто дождаться редиректа
//      if (isMobileFullscreen()) {
//        t.el.style.transform = "translateY(${G.window.innerHeight}px)";
//        G.window.setTimeout(function() {
//          if (removeOnHide) t.remove();
//          else t.clsOff(VisibleCss);
//        }, RemoveAfterTransformTime);
//      } else {
        // el.fadeOut(fadeSpeed, -> $(@).removeClass('shade').remove())
      containerT.clsOff(ContainerVisibleCss);
      G.window.setTimeout(function() {
        containerT.remove();
      }, RemoveAfterTransformTime);
//      }
    }
    isShown = false;
  }

  // ------------------------------- Private & protected methods -------------------------------

  private function isMobileFullscreen(): Bool return mobileFullScreen && WindowSize.mobile;

  private function updateClose() {
//    that = @
//    $((if rr.windowSize.mobile then '.close, header' else '.close'), @el).click () ->
//      that.hide()
    t.fndAnd('.close', function(t: Tag) {t.off('click', hide).on('click', hide);});
  }

  function setShade(shade: Bool) {
    containerT.setCls(ContainerShadeCss, shade);
  }
}
