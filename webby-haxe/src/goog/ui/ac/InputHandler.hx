package goog.ui.ac;

import goog.events.BrowserEvent;
import goog.events.Event;
import goog.events.EventTarget;
import haxe.extern.EitherType;
import haxe.extern.Rest;
import js.RegExp;
import js.html.Element;

/**
 * Class for managing the interaction between an auto-complete object and a
 * text-input or textarea.
 *
 * @param {?string=} opt_separators Separators to split multiple entries.
 *     If none passed, uses ',' and ';'.
 * @param {?string=} opt_literals Characters used to delimit text literals.
 * @param {?boolean=} opt_multi Whether to allow multiple entries
 *     (Default: true).
 * @param {?number=} opt_throttleTime Number of milliseconds to throttle
 *     keyevents with (Default: 150). Use -1 to disable updates on typing. Note
 *     that typing the separator will update autocomplete suggestions.
 * @constructor
 * @extends {goog.Disposable}
 */
@:jsRequire('goog.ui.ac.InputHandler')
extern class InputHandler extends Disposable {
  function new(?opt_separators: String, ?opt_literals: String, ?opt_multi: Bool, ?opt_throttleTime: Int);

  var throttleTime: Int;

/**
 * Standard list separators.
 * @type {string}
 * @const
 */
  static inline var STANDARD_LIST_SEPARATORS = ',;';


/**
 * Literals for quotes.
 * @type {string}
 * @const
 */
  static inline var QUOTE_LITERALS = '"';

/**
 * Attach an instance of an AutoComplete
 * @param {goog.ui.ac.AutoComplete} ac Autocomplete object.
 */
  function attachAutoComplete(ac: AutoComplete): Void;

/**
 * Returns the associated autocomplete instance.
 * @return {goog.ui.ac.AutoComplete} The associated autocomplete instance.
 */
  function getAutoComplete(): AutoComplete;

/**
 * Returns the current active element.
 * @return {Element} The currently active element.
 */
  function getActiveElement(): Element;

/**
 * Returns the value of the current active element.
 * @return {string} The value of the current active element.
 */
  function getValue(): String;

/**
 * Sets the value of the current active element.
 * @param {string} value The new value.
 */
  function setValue(value: String): Void;

/**
 * Returns the current cursor position.
 * @return {number} The index of the cursor position.
 */
  function getCursorPosition(): Int;

/**
 * Sets the cursor at the given position.
 * @param {number} pos The index of the cursor position.
 */
  function setCursorPosition(pos: Int): Void;

/**
 * Attaches the input handler to a target element. The target element
 * should be a textarea, input box, or other focusable element with the
 * same interface.
 * @param {Element|goog.events.EventTarget} target An element to attach the
 *     input handler to.
 */
  function attachInput(target: EitherType<Element, EventTarget>): Void;

/**
 * Detaches the input handler from the provided element.
 * @param {Element|goog.events.EventTarget} target An element to detach the
 *     input handler from.
 */
  function detachInput(target: EitherType<Element, EventTarget>): Void;

/**
 * Attaches the input handler to multiple elements.
 * @param {...Element} var_args Elements to attach the input handler too.
 */
  function attachInputs(var_args: Rest<Element>): Void;

/**
 * Detaches the input handler from multuple elements.
 * @param {...Element} var_args Variable arguments for elements to unbind from.
 */
  function detachInputs(var_args: Rest<Element>): Void;

/**
 * Selects the given row.  Implements the SelectionHandler interface.
 * @param {Object} row The row to select.
 * @param {boolean=} opt_multi Should this be treated as a single or multi-token
 *     auto-complete?  Overrides previous setting of opt_multi on constructor.
 * @return {boolean} Whether to suppress the update event.
 */
  function selectRow(row: Dynamic, ?opt_multi: Bool): Bool;

/**
 * Sets the text of the current token without updating the autocomplete
 * choices.
 * @param {string} tokenText The text for the current token.
 * @param {boolean=} opt_multi Should this be treated as a single or multi-token
 *     auto-complete?  Overrides previous setting of opt_multi on constructor.
 * @protected
 */
  @:protected function setTokenText(tokenText: String, ?opt_multi: Bool): Void;

/** @override */
  override function disposeInternal(): Void;

/**
 * Sets the entry separator characters.
 *
 * @param {string} separators The separator characters to set.
 * @param {string=} opt_defaultSeparators The defaultSeparator character to set.
 */
  function setSeparators(separators: String, ?opt_defaultSeparators: String): Void;

/**
 * Sets whether to flip the orientation of up & down for hiliting next
 * and previous autocomplete entries.
 * @param {boolean} upsideDown Whether the orientation is upside down.
 */
  function setUpsideDown(upsideDown: Bool): Void;

/**
 * Sets whether auto-completed tokens should be wrapped with whitespace.
 * @param {boolean} newValue boolean value indicating whether or not
 *     auto-completed tokens should be wrapped with whitespace.
 */
  function setWhitespaceWrapEntries(newValue: Bool): Void;

/**
 * Sets whether new tokens should be generated from literals.  That is, should
 * hello'world be two tokens, assuming ' is a literal?
 * @param {boolean} newValue boolean value indicating whether or not
 * new tokens should be generated from literals.
 */
  function setGenerateNewTokenOnLiteral(newValue: Bool): Void;

/**
 * Sets the regular expression used to trim the tokens before passing them to
 * the matcher:  every substring that matches the given regular expression will
 * be removed.  This can also be set to null to disable trimming.
 * @param {RegExp} trimmer Regexp to use for trimming or null to disable it.
 */
  function setTrimmingRegExp(trimmer: RegExp): Void;

/**
 * Sets whether we will prevent the default input behavior (moving focus to the
 * next focusable  element) on TAB.
 * @param {boolean} newValue Whether to preventDefault on TAB.
 */
  function setPreventDefaultOnTab(newValue: Bool): Void;

/**
 * Sets whether we will prevent highlighted item selection on TAB.
 * @param {boolean} newValue Whether to prevent selection on TAB.
 */
  function setPreventSelectionOnTab(newValue: Bool): Void;

/**
 * Sets whether separators perform autocomplete.
 * @param {boolean} newValue Whether to autocomplete on separators.
 */
  function setSeparatorCompletes(newValue: Bool): Void;

/**
 * Sets whether separators perform autocomplete.
 * @param {boolean} newValue Whether to autocomplete on separators.
 */
  function setSeparatorSelects(newValue: Bool): Void;

/**
 * Gets the time to wait before updating the results. If the update during
 * typing flag is switched on, this delay counts from the last update,
 * otherwise from the last keypress.
 * @return {number} Throttle time in milliseconds.
 */
  function getThrottleTime(): Int;

/**
 * Sets whether a row has just been selected.
 * @param {boolean} justSelected Whether or not the row has just been selected.
 */
  function setRowJustSelected(justSelected: Bool): Void;

/**
 * Sets the time to wait before updating the results.
 * @param {number} time New throttle time in milliseconds.
 */
  function setThrottleTime(time: Int): Void;

/**
 * Gets whether the result list is updated during typing.
 * @return {boolean} Value of the flag.
 */
  function getUpdateDuringTyping(): Bool;

/**
 * Sets whether the result list should be updated during typing.
 * @param {boolean} value New value of the flag.
 */
  function setUpdateDuringTyping(value: Bool): Void;

/**
 * Handles a key event.
 * @param {goog.events.BrowserEvent} e Browser event object.
 * @return {boolean} True if the key event was handled.
 * @protected
 */
  @:protected function handleKeyEvent(e: BrowserEvent): Void;


/**
 * @return {boolean} Whether this inputhandler need to listen on key-up.
 * @protected
 */
  @:protected function needKeyUpListener(): Bool;

/**
 * Handles the key up event. Registered only if needKeyUpListener returns true.
 * @param {goog.events.Event} e The keyup event.
 * @return {boolean} Whether an action was taken or not.
 * @protected
 */
  @:protected function handleKeyUp(e: Event): Bool;

/**
 * Handles an element getting focus.
 * @param {goog.events.Event} e Browser event object.
 * @protected
 */
  @:protected function handleFocus(e: Event): Void;

/**
 * Registers handlers for the active element when it receives focus.
 * @param {Element} target The element to focus.
 * @protected
 */
  @:protected function processFocus(target: Element): Void;

/**
 * Handles an element blurring.
 * @param {goog.events.Event=} opt_e Browser event object.
 * @protected
 */
  @:protected function handleBlur(?opt_e: Event): Void;

/**
 * Helper function that does the logic to handle an element blurring.
 * @protected
 */
  @:protected function processBlur(): Void;

/**
 * For subclasses to override to handle the mouse-down event.
 * @param {goog.events.BrowserEvent} e Browser event object.
 * @protected
 */
  @:protected function handleMouseDown(e: BrowserEvent): Void;

/**
 * Checks if an update has occurred and notified the autocomplete of the new
 * token.
 * @param {boolean=} opt_force If true the menu will be forced to update.
 */
  function update(?opt_force: Bool): Void;

/**
 * Parses a text area or input box for the currently highlighted token.
 * @return {string} Token to complete.
 * @protected
 */
  @:protected function parseToken(): String;
}
