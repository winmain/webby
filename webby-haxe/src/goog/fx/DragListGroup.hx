package goog.fx;

import goog.events.BrowserEvent;
import goog.events.Event;
import goog.events.EventTarget;
import goog.math.Coordinate;
import haxe.extern.EitherType;
import haxe.extern.Rest;
import js.html.Element;

/**
 * A class representing a group of one or more "drag lists" with items that can
 * be dragged within them and between them.
 *
 * Example usage:
 *   var dragListGroup = new goog.fx.DragListGroup();
 *   dragListGroup.setDragItemHandleHoverClass(className1, className2);
 *   dragListGroup.setDraggerElClass(className3);
 *   dragListGroup.addDragList(vertList, goog.fx.DragListDirection.DOWN);
 *   dragListGroup.addDragList(horizList, goog.fx.DragListDirection.RIGHT);
 *   dragListGroup.init();
 *
 * @extends {goog.events.EventTarget}
 * @constructor
 * @struct
 */
@:jsRequire('goog.fx.DragListGroup')
extern class DragListGroup extends EventTarget {
  function new();

/**
 * Sets the property of the currDragItem that it is always displayed in the
 * list.
 */
  function setIsCurrDragItemAlwaysDisplayed(): Void;


/**
 * Sets the private property updateWhileDragging_ to false. This disables the
 * update of the position of the currDragItem while dragging. It will only be
 * placed to its new location once the drag ends.
 */
  function setNoUpdateWhileDragging(): Void;

/**
 * Sets the distance the user has to drag the element before a drag operation
 * is started.
 * @param {number} distance The number of pixels after which a mousedown and
 *     move is considered a drag.
 */
  function setHysteresis(distance: Float): Void;


/**
 * @return {number} distance The number of pixels after which a mousedown and
 *     move is considered a drag.
 */
  function getHysteresis(): Float;


/**
 * Adds a drag list to this DragListGroup.
 * All calls to this method must happen before the call to init().
 * Remember that all child nodes (except text nodes) will be made draggable to
 * any other drag list in this group.
 *
 * @param {Element} dragListElement Must be a container for a list of items
 *     that should all be made draggable.
 * @param {goog.fx.DragListDirection} growthDirection The direction that this
 *     drag list grows in (i.e. if an item is appended to the DOM, the list's
 *     bounding box expands in this direction).
 * @param {boolean=} opt_unused Unused argument.
 * @param {string=} opt_dragHoverClass CSS class to apply to this drag list when
 *     the draggerEl hovers over it during a drag action.  If present, must be a
 *     single, valid classname (not a string of space-separated classnames).
 */
  function addDragList(dragListElement: Element, growthDirection: DragListDirection, ?opt_unused: Bool, ?opt_dragHoverClass: String): Void;


/**
 * Sets a user-supplied function used to get the "handle" element for a drag
 * item. The function must accept exactly one argument. The argument may be
 * any drag item element.
 *
 * If not set, the default implementation uses the whole drag item as the
 * handle.
 *
 * @param {function(Element): Element} getHandleForDragItemFn A function that,
 *     given any drag item, returns a reference to its "handle" element
 *     (which may be the drag item element itself).
 */
  function setFunctionToGetHandleForDragItem(getHandleForDragItemFn: Element -> Element): Void;


/**
 * Sets a user-supplied CSS class to add to a drag item on hover (not during a
 * drag action).
 * @param {...!string} var_args The CSS class or classes.
 */
  function setDragItemHoverClass(var_args: Rest<String>): Void;


/**
 * Sets a user-supplied CSS class to add to a drag item handle on hover (not
 * during a drag action).
 * @param {...!string} var_args The CSS class or classes.
 */
  function setDragItemHandleHoverClass(var_args: Rest<String>): Void;


/**
 * Sets a user-supplied CSS class to add to the current drag item (during a
 * drag action).
 *
 * If not set, the default behavior adds visibility:hidden to the current drag
 * item so that it is a block of empty space in the hover drag list (if any).
 * If this class is set by the user, then the default behavior does not happen
 * (unless, of course, the class also contains visibility:hidden).
 *
 * @param {...!string} var_args The CSS class or classes.
 */
  function setCurrDragItemClass(var_args: Rest<String>): Void;


/**
 * Sets a user-supplied CSS class to add to the clone of the current drag item
 * that's actually being dragged around (during a drag action).
 * @param {string} draggerElClass The CSS class.
 */
  function setDraggerElClass(draggerElClass: String): Void;


/**
 * Performs the initial setup to make all items in all lists draggable.
 */
  function init(): Void;


/**
 * Adds a single item to the given drag list and sets up the drag listeners for
 * it.
 * If opt_index is specified the item is inserted at this index, otherwise the
 * item is added as the last child of the list.
 *
 * @param {!Element} list The drag list where to add item to.
 * @param {!Element} item The new element to add.
 * @param {number=} opt_index Index where to insert the item in the list. If not
 * specified item is inserted as the last child of list.
 */
  function addItemToDragList(list: Element, item: Element, ?opt_index: Int): Void;


/** @override */
  override function disposeInternal(): Void;


/**
 * Caches the heights of each drag list and drag item, except for the current
 * drag item.
 *
 */
  function recacheListAndItemBounds(): Void;


/**
 * Listens for drag events on the given drag item. This method is currently used
 * to initialize drag items.
 *
 * @param {Element} dragItem the element to initialize. This element has to be
 * in one of the drag lists.
 * @protected
 */
  @:protected function listenForDragEvents(dragItem: Element): Void;


/**
 * Generates an element to follow the cursor during dragging, given a drag
 * source element.  The default behavior is simply to clone the source element,
 * but this may be overridden in subclasses.  This method is called by
 * {@code createDragElement()} before the drag class is added.
 *
 * @param {Element} sourceEl Drag source element.
 * @return {!Element} The new drag element.
 * @protected
 * @suppress {deprecated}
 */
  @:protected function createDragElementInternal(sourceEl: Element): Element;


/**
 * Updates the value of currHoverItem_.
 *
 * This method is used for insertion only when updateWhileDragging_ is false.
 * The below implementation is the basic one. This method can be extended by
 * a subclass to support changes to hovered item (eg: highlighting). Parametr
 * opt_draggerElCenter can be used for more sophisticated effects.
 *
 * @param {Element} hoverNextItem element of the list that is hovered over.
 * @param {goog.math.Coordinate=} opt_draggerElCenter current position of
 *     the dragged element.
 * @protected
 */
  @:protected function updateCurrHoverItem(hoverNextItem: Element, ?opt_draggerElCenter: Coordinate): Void;


/**
 * Inserts the currently dragged item in its new place.
 *
 * This method is used for insertion only when updateWhileDragging_ is false
 * (otherwise there is no need for that). In the basic implementation
 * the element is inserted before the currently hovered over item (this can
 * be changed by overriding the method in subclasses).
 *
 * @protected
 */
  @:protected function insertCurrHoverItem(): Void;
}


/**
 * Enum to indicate the direction that a drag list grows.
 * @enum {number}
 */
@:jsRequire('goog.fx.DragListDirection')
extern enum DragListDirection {
  DOWN;     // common
  RIGHT;    // common
  LEFT;     // uncommon (except perhaps for right-to-left interfaces)
  RIGHT_2D; // common + handles multiple lines if items are wrapped
  LEFT_2D;  // for rtl languages
}

/**
 * Events dispatched by this class.
 * @const
 */
@:jsRequire('goog.fx.DragListGroup.EventType')
extern class DragListGroupEventType {
  static var BEFOREDRAGSTART: String;
  static var DRAGSTART: String;
  static var BEFOREDRAGMOVE: String;
  static var DRAGMOVE: String;
  static var BEFOREDRAGEND: String;
  static var DRAGEND: String;
}


/**
 * The event object dispatched by DragListGroup.
 * The fields draggerElCenter, hoverList, and hoverNextItem are only available
 * for the BEFOREDRAGMOVE and DRAGMOVE events.
 *
 * @param {string} type The event type string.
 * @param {goog.fx.DragListGroup} dragListGroup A reference to the associated
 *     DragListGroup object.
 * @param {goog.events.BrowserEvent|goog.fx.DragEvent} event The event fired
 *     by the browser or fired by the dragger.
 * @param {Element} currDragItem The current drag item being moved.
 * @param {Element} draggerEl The clone of the current drag item that's actually
 *     being dragged around.
 * @param {goog.fx.Dragger} dragger The dragger object.
 * @param {goog.math.Coordinate=} opt_draggerElCenter The current center
 *     position of the draggerEl.
 * @param {Element=} opt_hoverList The current drag list that's being hovered
 *     over, or null if the center of draggerEl is outside of any drag lists.
 *     If not null and the drag action ends right now, then currDragItem will
 *     end up in this list.
 * @param {Element=} opt_hoverNextItem The current next item in the hoverList
 *     that the draggerEl is hovering over. (I.e. If the drag action ends
 *     right now, then this item would become the next item after the new
 *     location of currDragItem.) May be null if not applicable or if
 *     currDragItem would be added to the end of hoverList.
 * @constructor
 * @struct
 * @extends {goog.events.Event}
 */
extern class DragListGroupEvent extends Event {
  function new(
  type: String, dragListGroup: DragListGroup, event: EitherType<BrowserEvent, DragEvent>,
  currDragItem: Element, draggerEl: Element, dragger: Dragger,
  ?opt_draggerElCenter: Coordinate, ?opt_hoverList: Element, ?opt_hoverNextItem: Element);

  /**
   * A reference to the associated DragListGroup object.
   * @type {goog.fx.DragListGroup}
   */
  var dragListGroup: DragListGroup;

  /**
   * The event fired by the browser or fired by the dragger.
   * @type {goog.events.BrowserEvent|goog.fx.DragEvent}
   */
  var event: EitherType<BrowserEvent, DragEvent>;

  /**
   * The current drag item being move.
   * @type {Element}
   */
  var currDragItem: Element;

  /**
   * The clone of the current drag item that's actually being dragged around.
   * @type {Element}
   */
  var draggerEl: Element;

  /**
   * The dragger object.
   * @type {goog.fx.Dragger}
   */
  var dragger: Dragger;

  /**
   * The current center position of the draggerEl.
   * @type {goog.math.Coordinate|undefined}
   */
  var draggerElCenter: Coordinate;

  /**
   * The current drag list that's being hovered over, or null if the center of
   * draggerEl is outside of any drag lists. (I.e. If not null and the drag
   * action ends right now, then currDragItem will end up in this list.)
   * @type {Element|undefined}
   */
  var hoverList: Element;

  /**
   * The current next item in the hoverList that the draggerEl is hovering over.
   * (I.e. If the drag action ends right now, then this item would become the
   * next item after the new location of currDragItem.) May be null if not
   * applicable or if currDragItem would be added to the end of hoverList.
   * @type {Element|undefined}
   */
  var hoverNextItem: Element;
}
