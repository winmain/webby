package goog.fx;

import goog.events.BrowserEvent;
import goog.events.Event;

/**
 * Object representing a drag event
 * @param {string} type Event type.
 * @param {goog.fx.Dragger} dragobj Drag object initiating event.
 * @param {number} clientX X-coordinate relative to the viewport.
 * @param {number} clientY Y-coordinate relative to the viewport.
 * @param {goog.events.BrowserEvent} browserEvent The closure object
 *   representing the browser event that caused this drag event.
 * @param {number=} opt_actX Optional actual x for drag if it has been limited.
 * @param {number=} opt_actY Optional actual y for drag if it has been limited.
 * @param {boolean=} opt_dragCanceled Whether the drag has been canceled.
 * @constructor
 * @struct
 * @extends {goog.events.Event}
 */
@:jsRequire('goog.fx.DragEvent')
extern class DragEvent extends Event {
  function new(type: String, dragobj: Dragger, clientX: Int, clientY: Int, browserEvent: BrowserEvent,
  ?opt_actX: Int, ?opt_actY: Int, ?opt_dragCanceled: Bool);

  /**
   * X-coordinate relative to the viewport
   * @type {number}
   */
  var clientX: Int;

  /**
   * Y-coordinate relative to the viewport
   * @type {number}
   */
  var clientY: Int;

  /**
   * The closure object representing the browser event that caused this drag
   * event.
   * @type {goog.events.BrowserEvent}
   */
  var browserEvent: BrowserEvent;

  /**
   * The real x-position of the drag if it has been limited
   * @type {number}
   */
  var left: Int;

  /**
   * The real y-position of the drag if it has been limited
   * @type {number}
   */
  var top: Int;

  /**
   * Reference to the drag object for this event
   * @type {goog.fx.Dragger}
   */
  var dragger: Dragger;

  /**
   * Whether drag was canceled with this event. Used to differentiate between
   * a legitimate drag END that can result in an action and a drag END which is
   * a result of a drag cancelation. For now it can happen 1) with drag END
   * event on FireFox when user drags the mouse out of the window, 2) with
   * drag END event on IE7 which is generated on MOUSEMOVE event when user
   * moves the mouse into the document after the mouse button has been
   * released, 3) when TOUCHCANCEL is raised instead of TOUCHEND (on touch
   * events).
   * @type {boolean}
   */
  var dragCanceled: Bool;
}
