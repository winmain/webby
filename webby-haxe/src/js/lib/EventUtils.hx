package js.lib;

import js.html.Document;
import js.html.Node;

class EventUtils {

/**
 * Fire an event handler to the specified node. Event handlers can detect that the event was fired programatically
 * by testing for a 'synthetic=true' property on the event object
 * @param {HTMLNode} node The node to fire the event handler on.
 * @param {String} eventName The name of the event without the "on" (e.g., "focus")
 * @see https://stackoverflow.com/a/2381862/527467
 */
  public static function fireEvent(node: Node, eventName: String) {
    // Make sure we use the ownerDocument from the provided node to avoid cross-window problems
    var doc: Document;
    if (untyped node.ownerDocument) {
      doc = node.ownerDocument;
    } else if (node.nodeType == Node.DOCUMENT_NODE) {
      // the node may be the document itself, nodeType 9 = DOCUMENT_NODE
      doc = cast node;
    } else {
      throw new Error("Invalid node passed to fireEvent: " + node);
    }

    if (untyped node.dispatchEvent) {
      // Gecko-style approach (now the standard) takes more work
      var eventClass = "";

      // Different events have different event classes.
      // If this switch statement can't map an eventName to an eventClass,
      // the event firing is going to fail.
      switch (eventName) {
        // Dispatching of 'click' appears to not work correctly in Safari. Use 'mousedown' or 'mouseup' instead.
        case "click" | "mousedown" | "mouseup":
          eventClass = "MouseEvents";

        case "focus" | "change" | "blur" | "select":
          eventClass = "HTMLEvents";

        default:
          G.error("fireEvent: Couldn't find an event class for event '" + eventName + "'.");
      }
      var event = doc.createEvent(eventClass);
      event.initEvent(eventName, true, true); // All events created as bubbling and cancelable.

      untyped event.synthetic = true; // allow detection of synthetic events
      // The second parameter says go ahead with the default action
      (node : Dynamic).dispatchEvent(event, true);
    } else if (untyped node.fireEvent) {
      // IE-old school style, you can drop this if you don't need to support IE8 and lower
      var event = untyped doc.createEventObject();
      untyped event.synthetic = true; // allow detection of synthetic events
      untyped node.fireEvent("on" + eventName, event);
    }
  }
}
