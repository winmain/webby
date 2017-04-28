package js.lib;

using js.lib.ArrayUtils;

/*
Small helper class to ease initialization process with "onInit" callbacks support.
 */
class InitHandlers {
  public var initialized(default, null): Bool;
  private var initFns: Array<Void -> Void> = new Array();

  private var canResetInitialized: Bool;

  public function new(canResetInitialized: Bool = false) {
    this.canResetInitialized = canResetInitialized;
  }

  public function onInit(fn: Void -> Void) {
    if (initialized) fn();
    else initFns.pushUnique(fn);
  }

  public function removeHandler(fn: Void -> Void) {
    if (initFns != null) initFns.remove(fn);
  }

  public function doInit() {
    for (fn in initFns) {
      fn();
    }
    if (!canResetInitialized) initFns = null;
    initialized = true;
  }

  public function resetInitialized() {
    initialized = false;
  }
}
