package goog.math;

/**
 * Record for representing rectangular regions, allows compatibility between
 * things like ClientRect and goog.math.Rect.
 *
 * @record
 */
@:jsRequire('goog.math.IRect')
extern interface IRect {
/** @type {number} */
  var left: Float;

/** @type {number} */
  var top: Float;

/** @type {number} */
  var width: Float;

/** @type {number} */
  var height: Float;
}
