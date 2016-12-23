package goog.math;

import haxe.extern.EitherType;

/**
 * Class for representing rectangular regions.
 * @param {number} x Left.
 * @param {number} y Top.
 * @param {number} w Width.
 * @param {number} h Height.
 * @struct
 * @constructor
 * @implements {goog.math.IRect}
 */
extern class Rect implements IRect {
  function new(x: Float, y: Float, w: Float, h: Float);

/** @type {number} */
  var left: Float;

/** @type {number} */
  var top: Float;

/** @type {number} */
  var width: Float;

/** @type {number} */
  var height: Float;

/**
 * @return {!goog.math.Rect} A new copy of this Rectangle.
 */
  function clone(): Rect;


/**
 * Returns a new Box object with the same position and dimensions as this
 * rectangle.
 * @return {!goog.math.Box} A new Box representation of this Rectangle.
 */
  function toBox(): Box;


/**
 * Creates a new Rect object with the position and size given.
 * @param {!goog.math.Coordinate} position The top-left coordinate of the Rect
 * @param {!goog.math.Size} size The size of the Rect
 * @return {!goog.math.Rect} A new Rect initialized with the given position and
 *     size.
 */
  static function createFromPositionAndSize(position: Coordinate, size: Size): Rect;


/**
 * Creates a new Rect object with the same position and dimensions as a given
 * Box.  Note that this is only the inverse of toBox if left/top are defined.
 * @param {goog.math.Box} box A box.
 * @return {!goog.math.Rect} A new Rect initialized with the box's position
 *     and size.
 */
  static function createFromBox(box: Box): Rect;


  /**
   * Returns a nice string representing size and dimensions of rectangle.
   * @return {string} In the form (50, 73 - 75w x 25h).
   * @override
   */
  function toString(): String;


/**
 * Compares rectangles for equality.
 * @param {goog.math.IRect} a A Rectangle.
 * @param {goog.math.IRect} b A Rectangle.
 * @return {boolean} True iff the rectangles have the same left, top, width,
 *     and height, or if both are null.
 */
  static function equals(a: IRect, b: IRect): Bool;


/**
 * Computes the intersection of this rectangle and the rectangle parameter.  If
 * there is no intersection, returns false and leaves this rectangle as is.
 * @param {goog.math.IRect} rect A Rectangle.
 * @return {boolean} True iff this rectangle intersects with the parameter.
 */
  function intersection(rect: IRect): Bool;

/**
 * Returns the intersection of two rectangles. Two rectangles intersect if they
 * touch at all, for example, two zero width and height rectangles would
 * intersect if they had the same top and left.
 * @param {goog.math.IRect} a A Rectangle.
 * @param {goog.math.IRect} b A Rectangle.
 * @return {goog.math.Rect} A new intersection rect (even if width and height
 *     are 0), or null if there is no intersection.
 */
  static function intersection(a: IRect, b: IRect): Rect;

/**
 * Returns whether two rectangles intersect. Two rectangles intersect if they
 * touch at all, for example, two zero width and height rectangles would
 * intersect if they had the same top and left.
 * @param {goog.math.IRect} a A Rectangle.
 * @param {goog.math.IRect} b A Rectangle.
 * @return {boolean} Whether a and b intersect.
 */
  static function intersects(a: IRect, b: IRect): Bool;

/**
 * Returns whether a rectangle intersects this rectangle.
 * @param {goog.math.IRect} rect A rectangle.
 * @return {boolean} Whether rect intersects this rectangle.
 */
  function intersects(rect: IRect): Bool;


/**
 * Computes the difference regions between two rectangles. The return value is
 * an array of 0 to 4 rectangles defining the remaining regions of the first
 * rectangle after the second has been subtracted.
 * @param {goog.math.Rect} a A Rectangle.
 * @param {goog.math.IRect} b A Rectangle.
 * @return {!Array<!goog.math.Rect>} An array with 0 to 4 rectangles which
 *     together define the difference area of rectangle a minus rectangle b.
 */
  static function difference(a: Rect, b: IRect): Array<Rect>;


/**
 * Computes the difference regions between this rectangle and {@code rect}. The
 * return value is an array of 0 to 4 rectangles defining the remaining regions
 * of this rectangle after the other has been subtracted.
 * @param {goog.math.IRect} rect A Rectangle.
 * @return {!Array<!goog.math.Rect>} An array with 0 to 4 rectangles which
 *     together define the difference area of rectangle a minus rectangle b.
 */
  function difference(rect: IRect): Array<Rect>;


/**
 * Expand this rectangle to also include the area of the given rectangle.
 * @param {goog.math.IRect} rect The other rectangle.
 */
  function boundingRect(rect: IRect): Void;


/**
 * Returns a new rectangle which completely contains both input rectangles.
 * @param {goog.math.IRect} a A rectangle.
 * @param {goog.math.IRect} b A rectangle.
 * @return {goog.math.Rect} A new bounding rect, or null if either rect is
 *     null.
 */
  static function boundingRect(a: IRect, b: IRect): Rect;

/**
 * Tests whether this rectangle entirely contains another rectangle or
 * coordinate.
 *
 * @param {goog.math.IRect|goog.math.Coordinate} another The rectangle or
 *     coordinate to test for containment.
 * @return {boolean} Whether this rectangle contains given rectangle or
 *     coordinate.
 */
  function contains(another: EitherType<IRect, Coordinate>): Bool;


/**
 * @param {!goog.math.Coordinate} point A coordinate.
 * @return {number} The squared distance between the point and the closest
 *     point inside the rectangle. Returns 0 if the point is inside the
 *     rectangle.
 */
  function squaredDistance(point: Coordinate): Float;


/**
 * @param {!goog.math.Coordinate} point A coordinate.
 * @return {number} The distance between the point and the closest point
 *     inside the rectangle. Returns 0 if the point is inside the rectangle.
 */
  function distance(point: Coordinate): Float;


/**
 * @return {!goog.math.Size} The size of this rectangle.
 */
  function getSize(): Size;


/**
 * @return {!goog.math.Coordinate} A new coordinate for the top-left corner of
 *     the rectangle.
 */
  function getTopLeft(): Coordinate;


/**
 * @return {!goog.math.Coordinate} A new coordinate for the center of the
 *     rectangle.
 */
  function getCenter(): Coordinate;


/**
 * @return {!goog.math.Coordinate} A new coordinate for the bottom-right corner
 *     of the rectangle.
 */
  function getBottomRight(): Coordinate;


/**
 * Rounds the fields to the next larger integer values.
 * @return {!goog.math.Rect} This rectangle with ceil'd fields.
 */
  function ceil(): Rect;


/**
 * Rounds the fields to the next smaller integer values.
 * @return {!goog.math.Rect} This rectangle with floored fields.
 */
  function floor(): Rect;


/**
 * Rounds the fields to nearest integer values.
 * @return {!goog.math.Rect} This rectangle with rounded fields.
 */
  function round(): Rect;


/**
 * Translates this rectangle by the given offsets. If a
 * {@code goog.math.Coordinate} is given, then the left and top values are
 * translated by the coordinate's x and y values. Otherwise, top and left are
 * translated by {@code tx} and {@code opt_ty} respectively.
 * @param {number|goog.math.Coordinate} tx The value to translate left by or the
 *     the coordinate to translate this rect by.
 * @param {number=} opt_ty The value to translate top by.
 * @return {!goog.math.Rect} This rectangle after translating.
 */
  function translate(tx: EitherType<Float, Coordinate>, ?opt_ty: Float): Rect;


/**
 * Scales this rectangle by the given scale factors. The left and width values
 * are scaled by {@code sx} and the top and height values are scaled by
 * {@code opt_sy}.  If {@code opt_sy} is not given, then all fields are scaled
 * by {@code sx}.
 * @param {number} sx The scale factor to use for the x dimension.
 * @param {number=} opt_sy The scale factor to use for the y dimension.
 * @return {!goog.math.Rect} This rectangle after scaling.
 */
  function scale(sx: Float, ?opt_sy: Float): Rect;
}
