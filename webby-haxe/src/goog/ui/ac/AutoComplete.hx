package goog.ui.ac;

import goog.events.Event;
import goog.events.EventTarget;
import haxe.Constraints.Function;
import haxe.extern.EitherType;
import haxe.extern.Rest;
import js.html.Element;

/**
 * This is the central manager class for an AutoComplete instance. The matcher
 * can specify disabled rows that should not be hilited or selected by
 * implementing <code>isRowDisabled(row):boolean</code> for each autocomplete
 * row. No row will be considered disabled if this method is not implemented.
 *
 * @param {Object} matcher A data source and row matcher, implements
 *        <code>requestMatchingRows(token, maxMatches, matchCallback)</code>.
 * @param {goog.events.EventTarget} renderer An object that implements
 *        <code>
 *          isVisible():boolean<br>
 *          renderRows(rows:Array, token:string, target:Element);<br>
 *          hiliteId(row-id:number);<br>
 *          dismiss();<br>
 *          dispose():
 *        </code>.
 * @param {Object} selectionHandler An object that implements
 *        <code>
 *          selectRow(row);<br>
 *          update(opt_force);
 *        </code>.
 *
 * @constructor
 * @extends {goog.events.EventTarget}
 * @suppress {underscore}
 */
@:jsRequire("goog.ui.ac.AutoComplete")
extern class AutoComplete extends EventTarget {
  function new(matcher: Matcher, renderer: RendererTypedef, selectionHandler: SelectionHandler);


/**
 * @return {!Object} The data source providing the `autocomplete
 *     suggestions.
 */
  function getMatcher(): Matcher;


/**
 * Sets the data source providing the autocomplete suggestions.
 *
 * See constructor documentation for the interface.
 *
 * @param {!Object} matcher The matcher.
 * @protected
 */
  function setMatcher(matcher: Matcher): Void;


/**
 * @return {!Object} The handler used to interact with the input DOM
 *     element (textfield, textarea, or richedit), e.g. to update the
 *     input DOM element with selected value.
 * @protected
 */
  function getSelectionHandler(): SelectionHandler;


/**
 * @return {goog.events.EventTarget} The renderer that
 *     renders/shows/highlights/hides the autocomplete menu.
 *     See constructor documentation for the expected renderer API.
 */
  function getRenderer(): RendererTypedef;


/**
 * Sets the renderer that renders/shows/highlights/hides the autocomplete
 * menu.
 *
 * See constructor documentation for the expected renderer API.
 *
 * @param {goog.events.EventTarget} renderer The renderer.
 * @protected
 */
  function setRenderer(renderer: RendererTypedef): Void;


/**
 * @return {?string} The currently typed token used for completion.
 * @protected
 */
  function getToken(): String;


/**
 * Sets the current token (without changing the rendered autocompletion).
 *
 * NOTE(chrishenry): This method will likely go away when we figure
 * out a better API.
 *
 * @param {?string} token The new token.
 * @protected
 */
  function setTokenInternal(token: String): Void;


/**
 * @param {number} index The suggestion index, must be within the
 *     interval [0, this.getSuggestionCount()).
 * @return {Object} The currently suggested item at the given index
 *     (or null if there is none).
 */
  function getSuggestion(index: Int): Dynamic;


/**
 * @return {!Array<?>} The current autocomplete suggestion items.
 */
  function getAllSuggestions(): Array<Dynamic>;


/**
 * @return {number} The number of currently suggested items.
 */
  function getSuggestionCount(): Int;


/**
 * @return {number} The id (not index!) of the currently highlighted row.
 */
  function getHighlightedId(): Int;


/**
 * Generic event handler that handles any events this object is listening to.
 * @param {goog.events.Event} e Event Object.
 */
  function handleEvent(e: Dynamic): Void;


/**
 * Sets the max number of matches to fetch from the Matcher.
 *
 * @param {number} max Max number of matches.
 */
  function setMaxMatches(max: Int): Void;


/**
 * Sets whether or not the first row should be highlighted by default.
 *
 * @param {boolean} autoHilite true iff the first row should be
 *      highlighted by default.
 */
  function setAutoHilite(autoHilite: Bool): Void;


/**
 * Sets whether or not the up/down arrow can unhilite all rows.
 *
 * @param {boolean} allowFreeSelect true iff the up arrow can unhilite all rows.
 */
  function setAllowFreeSelect(allowFreeSelect: Bool): Void;


/**
 * Sets whether or not selections can wrap around the edges.
 *
 * @param {boolean} wrap true iff sections should wrap around the edges.
 */
  function setWrap(wrap: Bool): Void;


/**
 * Sets whether or not to request new suggestions immediately after completion
 * of a suggestion.
 *
 * @param {boolean} triggerSuggestionsOnUpdate true iff completion should fetch
 *     new suggestions.
 */
  function setTriggerSuggestionsOnUpdate(triggerSuggestionsOnUpdate: Bool): Void;


/**
 * Sets the token to match against.  This triggers calls to the Matcher to
 * fetch the matches (up to maxMatches), and then it triggers a call to
 * <code>renderer.renderRows()</code>.
 *
 * @param {string} token The string for which to search in the Matcher.
 * @param {string=} opt_fullString Optionally, the full string in the input
 *     field.
 */
  function setToken(token: String, ?opt_fullString: String): Void;


/**
 * Gets the current target HTML node for displaying autocomplete UI.
 * @return {Element} The current target HTML node for displaying autocomplete
 *     UI.
 */
  function getTarget(): Element;


/**
 * Sets the current target HTML node for displaying autocomplete UI.
 * Can be an implementation specific definition of how to display UI in relation
 * to the target node.
 * This target will be passed into  <code>renderer.renderRows()</code>
 *
 * @param {Element} target The current target HTML node for displaying
 *     autocomplete UI.
 */
  function setTarget(target: Element): Void;


/**
 * @return {boolean} Whether the autocomplete's renderer is open.
 */
  function isOpen(): Bool;


/**
 * Moves the hilite to the next non-disabled row.
 * Calls renderer.hiliteId() when there's something to do.
 * @return {boolean} Returns true on a successful hilite.
 */
  function hiliteNext(): Bool;

/**
 * Moves the hilite to the previous non-disabled row.  Calls
 * renderer.hiliteId() when there's something to do.
 * @return {boolean} Returns true on a successful hilite.
 */
  function hilitePrev(): Bool;


/**
 * Hilites the id if it's valid and the row is not disabled, otherwise does
 * nothing.
 * @param {number} id A row id (not index).
 * @return {boolean} Whether the id was hilited. Returns false if the row is
 *     disabled.
 */
  function hiliteId(id: Int): Bool;


/**
 * Hilites the index, if it's valid and the row is not disabled, otherwise does
 * nothing.
 * @param {number} index The row's index.
 * @return {boolean} Whether the index was hilited.
 */
  function hiliteIndex(index: Int): Bool;


/**
 * If there are any current matches, this passes the hilited row data to
 * <code>selectionHandler.selectRow()</code>
 * @return {boolean} Whether there are any current matches.
 */
  function selectHilited(): Bool;


/**
 * Returns whether or not the autocomplete is open and has a highlighted row.
 * @return {boolean} Whether an autocomplete row is highlighted.
 */
  function hasHighlight(): Bool;


/**
 * Clears out the token, rows, and hilite, and calls
 * <code>renderer.dismiss()</code>
 */
  function dismiss(): Void;


/**
 * Call a dismiss after a delay, if there's already a dismiss active, ignore.
 */
  function dismissOnDelay(): Void;


/**
 * Cancel the active delayed dismiss if there is one.
 */
  function cancelDelayedDismiss(): Void;

/** @override */
  override function disposeInternal(): Void;


/**
 * Renders the rows and adds highlighting.
 * @param {!Array<?>} rows Set of data that match the given token.
 * @param {(boolean|goog.ui.ac.RenderOptions)=} opt_options If true,
 *     keeps the currently hilited (by index) element hilited. If false not.
 *     Otherwise a RenderOptions object.
 */
  function renderRows(rows: Array<Dynamic>, ?opt_options: EitherType<Bool, RenderOptions>): Void;


/**
 * Gets the index corresponding to a particular id.
 * @param {number} id A unique id for the row.
 * @return {number} A valid index into rows_, or -1 if the id is invalid.
 * @protected
 */
  function getIndexOfId(id: Int): Int;


/**
 * Attach text areas or input boxes to the autocomplete by DOM reference.  After
 * elements are attached to the autocomplete, when a user types they will see
 * the autocomplete drop down.
 * @param {...Element} var_args Variable args: Input or text area elements to
 *     attach the autocomplete too.
 */
  function attachInputs(var_args: Rest<Element>): Void;


/**
 * Detach text areas or input boxes to the autocomplete by DOM reference.
 * @param {...Element} var_args Variable args: Input or text area elements to
 *     detach from the autocomplete.
 */
  function detachInputs(var_args: Rest<Element>): Void;


/**
 * Attaches the autocompleter to a text area or text input element
 * with an anchor element. The anchor element is the element the
 * autocomplete box will be positioned against.
 * @param {Element} inputElement The input element. May be 'textarea',
 *     text 'input' element, or any other element that exposes similar
 *     interface.
 * @param {Element} anchorElement The anchor element.
 */
  function attachInputWithAnchor(inputElement: Element, anchorElement: Element): Void;


/**
 * Forces an update of the display.
 * @param {boolean=} opt_force Whether to force an update.
 */
  function update(?opt_force: Bool): Void;
}


typedef RendererTypedef = {
/**
 * @return {boolean} True if the object is visible.
 */
  function isVisible(): Bool;

/**
 * Sets the anchor element for the subsequent call to renderRows.
 * @param {Element} anchor The anchor element.
 */
  function setAnchorElement(anchor: Element): Void;

/**
 * Render the autocomplete UI
 *
 * @param {Array<!Object>} rows Matching UI rows.
 * @param {string} token Token we are currently matching against.
 * @param {Element=} opt_target Current HTML node, will position popup beneath
 *     this node.
 */
  function renderRows(rows: Array<RendRow>, token: String, ?opt_target: Element): Void;

/**
 * Sets the 'active' class of the item with a given id.
 * @param {number} id Id of the row to hilight. If id is -1 then no rows get
 *     hilited.
 */
  function hiliteId(rowId: Int): Void;

/**
 * Hide the object.
 */
  function dismiss(): Void;

  function dispose(): Void;
}

typedef RendRow = {
  var id: Int;
  var data: Dynamic;
}


typedef SelectionHandler = {
  function selectRow(row: Dynamic, ?opt_multi: Bool): Bool;
  function update(?opt_force: Bool): Void;
}


/*
Класс событий, которые приходят от автокомплита.
 */
extern class RowEvent extends Event {
  public var row: Dynamic;
}

/**
 * Events associated with the autocomplete
 * @enum {string}
 */
@:jsRequire("goog.ui.ac.AutoComplete.EventType")
extern class EventType {
  /** A row has been highlighted by the renderer */
  static inline var ROW_HILITE = 'rowhilite';

  // Note: The events below are used for internal autocomplete events only and
  // should not be used in non-autocomplete code.

  /** A row has been mouseovered and should be highlighted by the renderer. */
  static inline var HILITE = 'hilite';

  /** A row has been selected by the renderer */
  static inline var SELECT = 'select';

  /** A dismiss event has occurred */
  static inline var DISMISS = 'dismiss';

  /** Event that cancels a dismiss event */
  static inline var CANCEL_DISMISS = 'canceldismiss';

  /**
   * Field value was updated.  A row field is included and is non-null when a
   * row has been selected.  The value of the row typically includes fields:
   * contactData and formattedValue as well as a toString function (though none
   * of these fields are guaranteed to exist).  The row field may be used to
   * return custom-type row data.
   */
  static inline var UPDATE = 'update';

  /**
   * The list of suggestions has been updated, usually because either the list
   * has opened, or because the user has typed another character and the
   * suggestions have been updated, or the user has dismissed the autocomplete.
   */
  static inline var SUGGESTIONS_UPDATE = 'suggestionsupdate';
}


/**
 * @typedef {{
 *   requestMatchingRows:(!Function|undefined),
 *   isRowDisabled:(!Function|undefined)
 * }}
 */
typedef Matcher = {
  @:optional function requestMatchingRows(token: String, maxMatches: Int, matchHandler: Function, ?opt_fullString: String): Void;
}

typedef Matcher2 = {
  @:optional function requestMatchingRows(token: String, maxMatches: Int, matchHandler: Function, ?opt_fullString: String): Void;
  @:optional function isRowDisabled(): Bool;
}
