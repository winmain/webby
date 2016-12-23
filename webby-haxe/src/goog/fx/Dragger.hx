package goog.fx;

import goog.events.EventTarget;
import goog.math.Coordinate;
import goog.math.Rect;
import js.html.Element;

/**
 * A class that allows mouse or touch-based dragging (moving) of an element
 *
 * @param {Element} target The element that will be dragged.
 * @param {Element=} opt_handle An optional handle to control the drag, if null
 *     the target is used.
 * @param {goog.math.Rect=} opt_limits Object containing left, top, width,
 *     and height.
 *
 * @extends {goog.events.EventTarget}
 * @constructor
 * @struct
 */
@:jsRequire('goog.fx.Dragger')
extern class Dragger extends EventTarget {
  function new(target: Element, ?opt_handle: Element, ?opt_limits: Rect);

  /**
   * Reference to drag target element.
   * @type {?Element}
   */
  var target: Element;

  /**
   * Reference to the handler that initiates the drag.
   * @type {?Element}
   */
  var handle: Element;

  /**
   * Object representing the limits of the drag region.
   * @type {goog.math.Rect}
   */
  var limits: Rect;

  /**
   * Current x position of mouse or touch relative to viewport.
   * @type {number}
   */
  var clientX: Int;

  /**
   * Current y position of mouse or touch relative to viewport.
   * @type {number}
   */
  var clientY: Int;

  /**
   * The x position where the first mousedown or touchstart occurred.
   * @type {number}
   */
  var startX: Int;

  /**
   * The y position where the first mousedown or touchstart occurred.
   * @type {number}
   */
  var startY: Int;

  /**
   * Current x position of drag relative to target's parent.
   * @type {number}
   */
  var deltaX: Int;

  /**
   * Current y position of drag relative to target's parent.
   * @type {number}
   */
  var deltaY: Int;

  /**
   * The current page scroll value.
   * @type {?goog.math.Coordinate}
   */
  var pageScroll: Coordinate;


/**
 * Creates copy of node being dragged.  This is a utility function to be used
 * wherever it is inappropriate for the original source to follow the mouse
 * cursor itself.
 *
 * @param {Element} sourceEl Element to copy.
 * @return {!Element} The clone of {@code sourceEl}.
 */
  static function cloneNode(sourceEl: Element): Element;


  // TODO: свойства и методы класса не импортированы

}
