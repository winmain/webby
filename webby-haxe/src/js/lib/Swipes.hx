package js.lib;

import js.html.TouchEvent;

/*
Swipe support on touch screen devices
 */
class Swipes {
  private static var initialized = false;

  private static inline var MinSwipeDistance = 30;

  private static var swipeListeners: Array<String -> Void> = [];

  public static function addListener(handler: String -> Void) {
    swipeListeners.push(handler);
    if (!initialized) init();
  }

  public static function removeListener(handler: String -> Void) {
    swipeListeners.remove(handler);
  }

  // ------------------------------- Private & protected methods -------------------------------

  private static function init(): Void {
    var xDown: Int = null, yDown: Int = null;
    var lastSwipeDir: String = null;

    function handleTouchStart(evt: TouchEvent) {
      xDown = evt.touches[0].clientX;
      yDown = evt.touches[0].clientY;
      lastSwipeDir = null;
    };

    function handleTouchMove(evt: TouchEvent) {
      if (xDown == null && yDown == null) {
        return;
      }
      var xDiff = evt.touches[0].clientX - xDown;
      var yDiff = evt.touches[0].clientY - yDown;

      if (Math.abs(xDiff) > Math.abs(yDiff)) {
        if (xDiff > MinSwipeDistance) {
          lastSwipeDir = 'right';
        } else if (xDiff < -MinSwipeDistance) {
          lastSwipeDir = 'left';
        }
      } else {
        if (yDiff > MinSwipeDistance) {
          lastSwipeDir = 'down';
        } else if (yDiff < MinSwipeDistance) {
          lastSwipeDir = 'up';
        }
      }
    };

    function handleTouchEnd(evt: TouchEvent) {
      if (lastSwipeDir != null) {
        fireListeners(lastSwipeDir);
        lastSwipeDir = null;
      }
    }

    G.document.addEventListener('touchstart', handleTouchStart, false);
    G.document.addEventListener('touchmove', handleTouchMove, false);
    G.document.addEventListener('touchend', handleTouchEnd, false);

    initialized = true;
  }

  private static function fireListeners(swipeDir: String) {
    for (listener in swipeListeners) {
      listener(swipeDir);
    }
  }
}
