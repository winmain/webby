package goog.math;

import haxe.extern.EitherType;

/**
 * Class for representing coordinates and positions.
 * @param {number=} opt_x Left, defaults to 0.
 * @param {number=} opt_y Top, defaults to 0.
 * @struct
 * @constructor
 */
@:jsRequire('goog.math.Coordinate')
extern class Coordinate {
  function new(?opt_x: Float, ?opt_y: Float);

  /**
   * X-value
   * @type {number}
   */
  var x: Float;

  /**
   * Y-value
   * @type {number}
   */
  var y: Float;

  /**
 * Returns a new copy of the coordinate.
 * @return {!goog.math.Coordinate} A clone of this coordinate.
 */
  function clone(): Coordinate;

  /**
   * Returns a nice string representing the coordinate.
   * @return {string} In the form (50, 73).
   * @override
   */
  function toString(): String;


/**
 * Compares coordinates for equality.
 * @param {goog.math.Coordinate} a A Coordinate.
 * @param {goog.math.Coordinate} b A Coordinate.
 * @return {boolean} True iff the coordinates are equal, or if both are null.
 */
  static function equals(a: Coordinate, b: Coordinate): Bool;


/**
 * Returns the distance between two coordinates.
 * @param {!goog.math.Coordinate} a A Coordinate.
 * @param {!goog.math.Coordinate} b A Coordinate.
 * @return {number} The distance between {@code a} and {@code b}.
 */
  static function distance(a: Coordinate, b: Coordinate): Float;


/**
 * Returns the magnitude of a coordinate.
 * @param {!goog.math.Coordinate} a A Coordinate.
 * @return {number} The distance between the origin and {@code a}.
 */
  static function magnitude(a: Coordinate): Float;


/**
 * Returns the angle from the origin to a coordinate.
 * @param {!goog.math.Coordinate} a A Coordinate.
 * @return {number} The angle, in degrees, clockwise from the positive X
 *     axis to {@code a}.
 */
  static function azimuth(a: Coordinate): Float;


/**
 * Returns the squared distance between two coordinates. Squared distances can
 * be used for comparisons when the actual value is not required.
 *
 * Performance note: eliminating the square root is an optimization often used
 * in lower-level languages, but the speed difference is not nearly as
 * pronounced in JavaScript (only a few percent.)
 *
 * @param {!goog.math.Coordinate} a A Coordinate.
 * @param {!goog.math.Coordinate} b A Coordinate.
 * @return {number} The squared distance between {@code a} and {@code b}.
 */
  static function squaredDistance(a: Coordinate, b: Coordinate): Float;


/**
 * Returns the difference between two coordinates as a new
 * goog.math.Coordinate.
 * @param {!goog.math.Coordinate} a A Coordinate.
 * @param {!goog.math.Coordinate} b A Coordinate.
 * @return {!goog.math.Coordinate} A Coordinate representing the difference
 *     between {@code a} and {@code b}.
 */
  static function difference(a: Coordinate, b: Coordinate): Coordinate;


/**
 * Returns the sum of two coordinates as a new goog.math.Coordinate.
 * @param {!goog.math.Coordinate} a A Coordinate.
 * @param {!goog.math.Coordinate} b A Coordinate.
 * @return {!goog.math.Coordinate} A Coordinate representing the sum of the two
 *     coordinates.
 */
  static function sum(a: Coordinate, b: Coordinate): Coordinate;


/**
 * Rounds the x and y fields to the next larger integer values.
 * @return {!goog.math.Coordinate} This coordinate with ceil'd fields.
 */
  function ceil(): Coordinate;


/**
 * Rounds the x and y fields to the next smaller integer values.
 * @return {!goog.math.Coordinate} This coordinate with floored fields.
 */
  function floor(): Coordinate;


/**
 * Rounds the x and y fields to the nearest integer values.
 * @return {!goog.math.Coordinate} This coordinate with rounded fields.
 */
  function round(): Coordinate;


/**
 * Translates this box by the given offsets. If a {@code goog.math.Coordinate}
 * is given, then the x and y values are translated by the coordinate's x and y.
 * Otherwise, x and y are translated by {@code tx} and {@code opt_ty}
 * respectively.
 * @param {number|goog.math.Coordinate} tx The value to translate x by or the
 *     the coordinate to translate this coordinate by.
 * @param {number=} opt_ty The value to translate y by.
 * @return {!goog.math.Coordinate} This coordinate after translating.
 */
  function translate(tx: EitherType<Float, Coordinate>, ?opt_ty: Float): Coordinate;


/**
 * Scales this coordinate by the given scale factors. The x and y values are
 * scaled by {@code sx} and {@code opt_sy} respectively.  If {@code opt_sy}
 * is not given, then {@code sx} is used for both x and y.
 * @param {number} sx The scale factor to use for the x dimension.
 * @param {number=} opt_sy The scale factor to use for the y dimension.
 * @return {!goog.math.Coordinate} This coordinate after scaling.
 */
  function scale(sx: Float, ?opt_sy: Float): Coordinate;


/**
 * Rotates this coordinate clockwise about the origin (or, optionally, the given
 * center) by the given angle, in radians.
 * @param {number} radians The angle by which to rotate this coordinate
 *     clockwise about the given center, in radians.
 * @param {!goog.math.Coordinate=} opt_center The center of rotation. Defaults
 *     to (0, 0) if not given.
 */
  function rotateRadians(radians: Float, ?opt_center: Coordinate): Void;

/**
 * Rotates this coordinate clockwise about the origin (or, optionally, the given
 * center) by the given angle, in degrees.
 * @param {number} degrees The angle by which to rotate this coordinate
 *     clockwise about the given center, in degrees.
 * @param {!goog.math.Coordinate=} opt_center The center of rotation. Defaults
 *     to (0, 0) if not given.
 */
  function rotateDegrees(degrees: Float, ?opt_center: Coordinate): Void;
}
