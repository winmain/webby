package goog.math;

/**
 * Class for representing sizes consisting of a width and height. Undefined
 * width and height support is deprecated and results in compiler warning.
 * @param {number} width Width.
 * @param {number} height Height.
 * @struct
 * @constructor
 */
@:jsRequire('goog.math.Size')
extern class Size {
  public function new(width: Float, height: Float);

  /**
   * Width
   * @type {number}
   */
  var width: Float;

  /**
   * Height
   * @type {number}
   */
  var height: Float;

  // TODO: импортировать методы
}
