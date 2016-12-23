package goog.events;

import haxe.extern.EitherType;

/**
 * A base class for event objects, so that they can support preventDefault and
 * stopPropagation.
 *
 * @suppress {underscore} Several properties on this class are technically
 *     public, but referencing these properties outside this package is strongly
 *     discouraged.
 *
 * @param {string|!goog.events.EventId} type Event Type.
 * @param {Object=} opt_target Reference to the object that is the target of
 *     this event. It has to implement the {@code EventTarget} interface
 *     declared at {@link http://developer.mozilla.org/en/DOM/EventTarget}.
 * @constructor
 */
@:jsRequire('goog.events.Event')
extern class Event {
  function new(type: EitherType<String, EventId<Dynamic>>, ?opt_target: Dynamic);

  /**
   * Event type.
   * @type {string}
   */
  var type: String;

  /**
   * TODO(tbreisacher): The type should probably be
   * EventTarget|goog.events.EventTarget.
   *
   * Target of the event.
   * @type {Object|undefined}
   */
  var target: Dynamic;

  /**
   * Object that had the listener attached.
   * @type {Object|undefined}
   */
  var currentTarget: Dynamic;


/**
 * Stops event propagation.
 */
  function stopPropagation(): Void;


/**
 * Prevents the default action, for example a link redirecting to a url.
 */
  function preventDefault(): Void;


/**
 * Stops the propagation of the event. It is equivalent to
 * {@code e.stopPropagation()}, but can be used as the callback argument of
 * {@link goog.events.listen} without declaring another function.
 * @param {!goog.events.Event} e An event.
 */
  static function stopPropagation(e: Event): Void;


/**
 * Prevents the default action. It is equivalent to
 * {@code e.preventDefault()}, but can be used as the callback argument of
 * {@link goog.events.listen} without declaring another function.
 * @param {!goog.events.Event} e An event.
 */
  static function preventDefault(e: Event): Void;
}
