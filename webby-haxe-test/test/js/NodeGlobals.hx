package js;

class NodeGlobals {
  public static function init(): Void {
    untyped __js__("
const Window = require('window');
global.window = new Window();

require('jsdom-global')();
");
  }
}
