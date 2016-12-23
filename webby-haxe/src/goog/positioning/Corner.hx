package goog.positioning;

/**
 * Enum for representing an element corner for positioning the popup.
 *
 * The START constants map to LEFT if element directionality is left
 * to right and RIGHT if the directionality is right to left.
 * Likewise END maps to RIGHT or LEFT depending on the directionality.
 *
 * @enum {number}
 */
@:jsRequire('goog.positioning.Corner')
extern enum Corner {
  TOP_LEFT;
  TOP_RIGHT;
  BOTTOM_LEFT;
  BOTTOM_RIGHT;
  TOP_START;
  TOP_END;
  BOTTOM_START;
  BOTTOM_END;
  TOP_CENTER;
  BOTTOM_CENTER;
}
