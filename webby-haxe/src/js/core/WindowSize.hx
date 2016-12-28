package js.core;

class WindowSize {
  public static inline var MaxWidthMobile = 640;
  public static inline var MaxWidthTablet = 1024;

  // Флаг мобильной версии. Если false, то мы сейчас в десктопной версии
  public static var mobile(default, null): Bool;

  // Ширина внутреннего окна браузера
  public static var width(default, null): Int;

  // Высота внутреннего окна браузера
  public static var height(default, null): Int;

  public static function init(): Void {
    G.window.addEventListener('resize', onResize);
    onResize();
  }

  private static function onResize(): Void {
    // Получить размеры окна, see http://stackoverflow.com/a/11744120/527467
    width = untyped G.window.innerWidth || G.document.documentElement.clientWidth || G.document.body.clientWidth;
    height = untyped G.window.innerHeight || G.document.documentElement.clientHeight || G.document.body.clientHeight;

    mobile = width <= MaxWidthMobile;
  }

}
