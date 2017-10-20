package goog.ui.ac;

import goog.events.EventTarget;
import goog.positioning.Corner;
import goog.ui.ac.AutoComplete.RendRow;
import haxe.extern.EitherType;
import js.html.Element;
import js.html.Node;

/**
 * Class for rendering the results of an auto-complete in a drop down list.
 *
 * @constructor
 * @param {Element=} opt_parentNode optional reference to the parent element
 *     that will hold the autocomplete elements. goog.dom.getDocument().body
 *     will be used if this is null.
 * @param {?({renderRow}|{render})=} opt_customRenderer Custom full renderer to
 *     render each row. Should be something with a renderRow or render method.
 * @param {boolean=} opt_rightAlign Determines if the autocomplete will always
 *     be right aligned. False by default.
 * @param {boolean=} opt_useStandardHighlighting Determines if standard
 *     highlighting should be applied to each row of data. Standard highlighting
 *     bolds every matching substring for a given token in each row. True by
 *     default.
 * @extends {goog.events.EventTarget}
 * @suppress {underscore}
 */
@:jsRequire('goog.ui.ac.Renderer')
extern class Renderer extends EventTarget {
  function new(?opt_parentNode: Element, ?opt_customRenderer: CustomRenderer, ?opt_rightAlign: Bool, ?opt_useStandardHighlighting: Bool);

  /**
   * Array of the node divs that hold each result that is being displayed.
   * @type {Array<Element>}
   * @protected
   * @suppress {underscore|visibility}
   */
  @:protected var rowDivs_: Array<Element>;

  /**
   * The index of the currently highlighted row
   * @type {number}
   * @protected
   * @suppress {underscore|visibility}
   */
  @:protected var hilitedRow_: Int;

  /**
   * The time that the rendering of the menu rows started
   * @type {number}
   * @protected
   * @suppress {underscore|visibility}
   */
  @:protected var startRenderingRows_: Int;

  /**
   * Classname for the main element.  This must be a single valid class name.
   * @type {string}
   */
  var className: String;

  /**
   * Classname for row divs.  This must be a single valid class name.
   * @type {string}
   */
  var rowClassName: String;

  /**
   * Class name for active row div.  This must be a single valid class name.
   * Active row will have rowClassName & activeClassName &
   * legacyActiveClassName.
   * @type {string}
   */
  var activeClassName: String;

  /**
   * Class name for the bold tag highlighting the matched part of the text.
   * @type {string}
   */
  var highlightedClassName: String;

  /**
   * Animation in progress, if any.
   * @type {goog.fx.Animation|undefined}
   */
  var animation_: Dynamic;

/**
 * The anchor element to position the rendered autocompleter against.
 * @protected {Element|undefined}
 */
  @:protected var target_: Element;

  /**
 * The delay before mouseover events are registered, in milliseconds
 * @type {number}
 * @const
 */
  static inline var DELAY_BEFORE_MOUSEOVER = 300;

/**
 * Gets the renderer's element.
 * @return {Element} The  main element that controls the rendered autocomplete.
 */
  function getElement(): Element;

/**
 * Sets the width provider element. The provider is only used on redraw and as
 * such will not automatically update on resize.
 * @param {Node} widthProvider The element whose width should be mirrored.
 */
  function setWidthProvider(widthProvider: Node): Void;

/**
 * Set whether to align autocomplete to top of target element
 * @param {boolean} align If true, align to top.
 */
  function setTopAlign(align: Bool): Void;

/**
 * @return {boolean} Whether we should be aligning to the top of
 *     the target element.
 */
  function getTopAlign(): Bool;

/**
 * Set whether to align autocomplete to the right of the target element.
 * @param {boolean} align If true, align to right.
 */
  function setRightAlign(align: Bool): Void;

/**
 * @return {boolean} Whether the autocomplete menu should be right aligned.
 */
  function getRightAlign(): Bool;

/**
 * @param {boolean} show Whether we should limit the dropdown from extending
 *     past the bottom of the screen and instead show a scrollbar on the
 *     dropdown.
 */
  function setShowScrollbarsIfTooLarge(show: Bool): Void;

/**
 * Set whether or not standard highlighting should be used when rendering rows.
 * @param {boolean} useStandardHighlighting true if standard highlighting used.
 */
  function setUseStandardHighlighting(useStandardHighlighting: Bool): Void;

/**
 * @param {boolean} matchWordBoundary Determines whether matches should be
 *     higlighted only when the token matches text at a whole-word boundary.
 *     True by default.
 */
  function setMatchWordBoundary(matchWordBoundary: Bool): Void;

/**
 * Set whether or not to highlight all matching tokens rather than just the
 * first.
 * @param {boolean} highlightAllTokens Whether to highlight all matching tokens
 *     rather than just the first.
 */
  function setHighlightAllTokens(highlightAllTokens: Bool): Void;

/**
 * Sets the duration (in msec) of the fade animation when menu is shown/hidden.
 * Setting to 0 (default) disables animation entirely.
 * @param {number} duration Duration (in msec) of the fade animation (or 0 for
 *     no animation).
 */
  function setMenuFadeDuration(duration: Int): Void;

/**
 * Sets the anchor element for the subsequent call to renderRows.
 * @param {Element} anchor The anchor element.
 */
  function setAnchorElement(anchor: Element): Void;

/**
 * @return {Element} The anchor element.
 * @protected
 */
  function getAnchorElement(): Element;

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
 * Hide the object.
 */
  function dismiss(): Void;

/**
 * Show the object.
 */
  function show(): Void;


/**
 * @return {boolean} True if the object is visible.
 */
  function isVisible(): Bool;

/**
 * Sets the 'active' class of the nth item.
 * @param {number} index Index of the item to highlight.
 */
  function hiliteRow(index: Int): Void;

/**
 * Removes the 'active' class from the currently selected row.
 */
  function hiliteNone(): Void;

/**
 * Sets the 'active' class of the item with a given id.
 * @param {number} id Id of the row to hilight. If id is -1 then no rows get
 *     hilited.
 */
  function hiliteId(id: Int): Void;

/**
 * Redraw (or draw if this is the first call) the rendered auto-complete drop
 * down.
 */
  function redraw(): Void;

/**
 * @return {goog.positioning.Corner} The anchor corner to position the popup at.
 * @protected
 */
  @:protected function getAnchorCorner(): Corner;

/**
 * Repositions the auto complete popup relative to the location node, if it
 * exists and the auto position has been set.
 */
  function reposition(): Void;

/**
 * Sets whether the renderer should try to determine where to position the
 * drop down.
 * @param {boolean} auto Whether to autoposition the drop down.
 */
  function setAutoPosition(auto: Bool): Void;

/**
 * @return {boolean} Whether the drop down will be autopositioned.
 * @protected
 */
  @:protected function getAutoPosition(): Bool;

/**
 * @return {Element} The target element.
 * @protected
 */
  @:protected function getTarget(): Element;

/**
 * Disposes of the renderer and its associated HTML.
 * @override
 * @protected
 */
  @:protected override function disposeInternal(): Void;

/**
 * Generic function that takes a row and renders a DOM structure for that row.
 *
 * Normally this will only be matching a maximum of 20 or so items.  Even with
 * 40 rows, DOM this building is fine.
 *
 * @param {Object} row Object representing row.
 * @param {string} token Token to highlight.
 * @param {Node} node The node to render into.
 * @private
 */
  @:protected function renderRowContents_(row: Dynamic, token: String, node: Node): Void;

/**
 * Goes through a node and all of its child nodes, replacing HTML text that
 * matches a token with <b>token</b>.
 * The replacement will happen on the first match or all matches depending on
 * this.highlightAllTokens_ value.
 *
 * @param {Node} node Node to match.
 * @param {string|Array<string>} tokenOrArray Token to match or array of tokens
 *     to match.  By default, only the first match will be highlighted.  If
 *     highlightAllTokens is set, then all tokens appearing at the start of a
 *     word, in whatever order and however many times, will be highlighted.
 * @private
 */
  @:protected function startHiliteMatchingText_(node: Node, tokenOrArray: EitherType<String, Array<String>>): Void;

/**
 * @param {Node} node Node to match.
 * @param {string|Array<string>} tokenOrArray Token to match or array of tokens
 *     to match.
 * @private
 */
  @:protected function hiliteMatchingText_(node: Node, tokenOrArray: EitherType<String, Array<String>>): Void;

/**
 * Transforms a token into a string ready to be put into the regular expression
 * in hiliteMatchingText_.
 * @param {string|Array<string>} tokenOrArray The token or array to get the
 *     regex string from.
 * @return {string} The regex-ready token.
 * @private
 */
  @:protected function getTokenRegExp_(tokenOrArray: EitherType<String, Array<String>>): String;

/**
 * Render a row by creating a div and then calling row rendering callback or
 * default row handler
 *
 * @param {Object} row Object representing row.
 * @param {string} token Token to highlight.
 * @return {!Element} An element with the rendered HTML.
 */
  function renderRowHtml(row: RendRow, token: String): Element;


  // ------------------------------- Private & protected methods -------------------------------

  /**
   * Flag to set all tokens as highlighted in the autocomplete row.
   * @type {boolean}
   * @private
   */
  @:protected var highlightAllTokens_: Bool;
}

typedef CustomRenderer = {
  @:optional function render(renderer: Renderer, elem: Element, rows: Array<RendRow>, token: String): Void;
  @:optional function renderRow(row: RendRow, token: String, elem: Element): Void;
}
