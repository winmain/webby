package goog.disposable;

/**
 * Interface for a disposable object.  If a instance requires cleanup
 * (references COM objects, DOM notes, or other disposable objects), it should
 * implement this interface (it may subclass goog.Disposable).
 * @interface
 */
@:jsRequire('goog.disposable.IDisposable')
extern interface IDisposable {
/**
 * Disposes of the object and its resources.
 * @return {void} Nothing.
 */
  function dispose(): Void;


/**
 * @return {boolean} Whether the object has been disposed of.
 */
  function isDisposed(): Bool;
}
