package js.mdl;

import haxe.extern.EitherType;
import js.html.Element;
import js.html.HTMLCollection;
import js.html.HtmlElement;
import js.html.Node;
import js.html.NodeList;

/**
 * A component handler interface using the revealing module design pattern.
 * More details on this design pattern here:
 * https://github.com/jasonmayes/mdl-component-design-pattern
 */
@:native('componentHandler')
extern class ComponentHandler {
  /**
   * Searches existing DOM for elements of our component type and upgrades them
   * if they have not already been upgraded.
   *
   * @param {string=} optJsClass the programatic name of the element class we
   * need to create a new instance of.
   * @param {string=} optCssClass the name of the CSS class elements of this
   * type will have.
   */
  static function upgradeDom(?optJsClass: String, ?optCssClass: String): Void;

  /**
   * Upgrades a specific element rather than all in the DOM.
   *
   * @param {!Element} element The element we wish to upgrade.
   * @param {string=} optJsClass Optional name of the class we want to upgrade
   * the element to.
   */
  static function upgradeElement(element: Element, ?optJsClass: String): Void;

  /**
   * Upgrades a specific list of elements rather than all in the DOM.
   *
   * @param {!Element|!Array<!Element>|!NodeList|!HTMLCollection} elements
   * The elements we wish to upgrade.
   */
  static function upgradeElements(elements: EitherType<Element, EitherType<Array<Element>, EitherType<NodeList, HTMLCollection>>>): Void;

  /**
   * Upgrades all registered components found in the current DOM. This is
   * automatically called on window load.
   */
  static function upgradeAllRegistered(): Void;

  /**
   * Allows user to be alerted to any upgrades that are performed for a given
   * component type
   *
   * @param {string} jsClass The class name of the MDL component we wish
   * to hook into for any upgrades performed.
   * @param {function(!HTMLElement)} callback The function to call upon an
   * upgrade. This function should expect 1 parameter - the HTMLElement which
   * got upgraded.
   */
  static function registerUpgradedCallback(jsClass: String, callback: HtmlElement -> Void): Void;

  /**
   * Registers a class for future use and attempts to upgrade existing DOM.
   *
   * @param {componentHandler.ComponentConfigPublic} config the registration configuration
   */
  static function register(config: Dynamic): Void;

  /**
   * Downgrade either a given node, an array of nodes, or a NodeList.
   *
   * @param {!Node|!Array<!Node>|!NodeList} nodes
   */
  static function downgradeElements(nodes: EitherType<Node, EitherType<Array<Node>, NodeList>>): Void;
}
