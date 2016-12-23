package goog.ui.ac;

/**
 * A simple class that contains options for rendering a set of autocomplete
 * matches.  Used as an optional argument in the callback from the matcher.
 * @constructor
 */
@:jsRequire('goog.ui.ac.RenderOptions')
extern class RenderOptions {
  function new();

/**
 * @param {boolean} flag The new value for the preserveHilited_ flag.
 */
  function setPreserveHilited(flag: Bool): Void;


/**
 * @return {boolean} The value of the preserveHilited_ flag.
 */
  function getPreserveHilited(): Bool;


/**
 * @param {boolean} flag The new value for the autoHilite_ flag.
 */
  function setAutoHilite(flag: Bool): Void;


/**
 * @return {boolean|undefined} The value of the autoHilite_ flag.
 */
  function getAutoHilite(): Bool;
}
