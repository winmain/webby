package goog.math;

/**
 * Class for representing a box. A box is specified as a top, right, bottom,
 * and left. A box is useful for representing margins and padding.
 *
 * This class assumes 'screen coordinates': larger Y coordinates are further
 * from the top of the screen.
 *
 * @param {number} top Top.
 * @param {number} right Right.
 * @param {number} bottom Bottom.
 * @param {number} left Left.
 * @struct
 * @constructor
 */
@:jsRequire('goog.math.Box')
extern class Box {
  public function new(top: Float, right: Float, bottom: Float, left: Float);

  /**
   * Top
   * @type {number}
   */
  var top: Float;

  /**
   * Right
   * @type {number}
   */
  var right: Float;

  /**
   * Bottom
   * @type {number}
   */
  var bottom: Float;

  /**
   * Left
   * @type {number}
   */
  var left: Float;

  // TODO: имортировать методы
}
