package js.lib;

/*
Small helper class to ease initialization process with "onInit" callbacks support.
 */
class InitHandlers {
  public var initialized(default, null): Bool;
  private var initFns: Array<Void -> Void> = new Array();

  public function new() {
  }

  public function onInit(fn: Void -> Void) {
    if (initialized) fn();
    else initFns.push(fn);
  }

  public function doInit() {
    for (fn in initFns) {
      fn();
    }
    initFns = null;
    initialized = true;
  }
}
