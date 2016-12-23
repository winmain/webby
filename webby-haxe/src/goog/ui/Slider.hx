package goog.ui;

import goog.dom.DomHelper;
import haxe.Constraints.Function;

/**
 * @fileoverview A slider implementation that allows to select a value within a
 * range by dragging a thumb. The selected value is exposed through getValue().
 *
 * To decorate, the slider should be bound to an element with the class name
 * 'goog-slider' containing a child with the class name 'goog-slider-thumb',
 * whose position is set to relative.
 * Note that you won't be able to see these elements unless they are styled.
 *
 * Slider orientation is horizontal by default.
 * Use setOrientation(goog.ui.Slider.Orientation.VERTICAL) for a vertical
 * slider.
 *
 * Decorate Example:
 * <div id="slider" class="goog-slider">
 *   <div class="goog-slider-thumb"></div>
 * </div>
 *
 * JavaScript code:
 * <code>
 *   var slider = new goog.ui.Slider;
 *   slider.decorate(document.getElementById('slider'));
 * </code>
 *
 * @author arv@google.com (Erik Arvidsson)
 * @see ../demos/slider.html
 */
@:jsRequire('goog.ui.Slider')
extern class Slider extends SliderBase {
/**
 * This creates a slider object.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @param {(function(number):?string)=} opt_labelFn An optional function mapping
 *     slider values to a description of the value.
 * @constructor
 * @extends {goog.ui.SliderBase}
 */
  public function new(?opt_domHelper: DomHelper, ?opt_labelFn: Function);

  // TODO: нет методов
}
