package goog.ui;

import goog.dom.DomHelper;
import haxe.Constraints.Function;

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
@:jsRequire('goog.ui.SliderBase')
extern class SliderBase extends Component {
/**
 * This creates a SliderBase object.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @param {(function(number):?string)=} opt_labelFn An optional function mapping
 *     slider values to a description of the value.
 * @constructor
 * @extends {goog.ui.Component}
 */
  public function new(?opt_domHelper: DomHelper, ?opt_labelFn: Function);

  // TODO: нет методов
}
