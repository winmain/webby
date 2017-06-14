package js.lib;

class TestUpload {
  /*
  Можно ли делать заливку файлов?
   */
  public static function testUpload(): Bool return untyped __js__("('draggable' in document.createElement('span')) && !!window['FormData']");

  /*
  Работает ли встроенный механизм прогресса заливки?
   */
  public static function testProgress(): Bool return untyped __js__('"upload" in new XMLHttpRequest');
}
