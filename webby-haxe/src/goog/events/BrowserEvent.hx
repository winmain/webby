package goog.events;

/**
 * Accepts a browser event object and creates a patched, cross browser event
 * object.
 * The content of this object will not be initialized if no event object is
 * provided. If this is the case, init() needs to be invoked separately.
 * @param {Event=} opt_e Browser event object.
 * @param {EventTarget=} opt_currentTarget Current target for event.
 * @constructor
 * @extends {goog.events.Event}
 */
@:jsRequire('goog.events.BrowserEvent')
extern class BrowserEvent extends Event {
//  function new() {
//  }
  // TODO:
}
