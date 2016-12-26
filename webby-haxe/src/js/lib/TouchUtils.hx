package js.lib;

import js.html.Event;

class TouchUtils {
  /* Поддерживает ли устройство тач? Эта переменная выставляется не сразу, а как только придёт первое тач событие */
  public static var touchSupported(default, null): Bool = false;

  public static function init(): Void {
    detectTouchCapability();
    iOSDisableZoomAndScroll();
  }

  /*
  Включаем обнаружение тач-способностей у нашего девайса.
  Это особенно актуально для шляпофонов с их странной логикой эмуляции mouseover, которая описана здесь
  http://stackoverflow.com/questions/3038898/ipad-iphone-hover-problem-causes-the-user-to-double-click-a-link
   */
  public static function detectTouchCapability() {
    G.document.addEventListener('touchstart', function() {
      touchSupported = true;
    }, untyped {"once": true});
  }

  /*
  Выключаем зум и скролл страницы в iOS 10, see http://stackoverflow.com/a/39711930/527467
   */
  public static function iOSDisableZoomAndScroll(): Void {
    G.document.addEventListener('gesturestart', function(e: Event) {
      e.preventDefault();
    });
  }
}
