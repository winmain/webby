package goog.dom;

import js.html.Document;

@:jsRequire('goog.dom.DomHelper')
extern class DomHelper {
/**
 * Create an instance of a DOM helper with a new document object.
 * @param {Document=} opt_document Document object to associate with this
 *     DOM helper.
 * @constructor
 */
  public function new(?opt_document: Document);

  // TODO: если понадобится, надо импортировать методы сюда
}
