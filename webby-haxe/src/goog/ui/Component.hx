package goog.ui;

import goog.dom.DomHelper;
import goog.events.EventTarget;

/**
 * Default implementation of UI component.
 *
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.events.EventTarget}
 * @suppress {underscore}
 */
@:jsRequire('goog.ui.Component')
extern class Component extends EventTarget {
  public function new(?opt_domHelper: DomHelper);
}
