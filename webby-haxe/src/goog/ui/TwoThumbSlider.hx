package goog.ui;

import goog.dom.DomHelper;

/**
 * @fileoverview Twothumbslider is a slider that allows to select a subrange
 * within a range by dragging two thumbs. The selected sub-range is exposed
 * through getValue() and getExtent().
 *
 * To decorate, the twothumbslider should be bound to an element with the class
 * name 'goog-twothumbslider-[vertical / horizontal]' containing children with
 * the classname 'goog-twothumbslider-value-thumb' and
 * 'goog-twothumbslider-extent-thumb', respectively.
 *
 * Decorate Example:
 * <div id="twothumbslider" class="goog-twothumbslider-horizontal">
 *   <div class="goog-twothumbslider-value-thumb">
 *   <div class="goog-twothumbslider-extent-thumb">
 * </div>
 * <script>
 *
 * var slider = new goog.ui.TwoThumbSlider;
 * slider.decorate(document.getElementById('twothumbslider'));
 *
 * TODO(user): add a11y once we know what this element is
 *
 * @see ../demos/twothumbslider.html
 */
@:jsRequire('goog.ui.TwoThumbSlider')
extern class TwoThumbSlider extends SliderBase {
/**
 * This creates a TwoThumbSlider object.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.SliderBase}
 */
  public function new(?opt_domHelper: DomHelper);

  // TODO: нет методов
}
