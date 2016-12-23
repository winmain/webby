package goog;

import goog.disposable.IDisposable;
import haxe.Constraints.Function;

/**
 * Class that provides the basic implementation for disposable objects. If your
 * class holds one or more references to COM objects, DOM nodes, or other
 * disposable objects, it should extend this class or implement the disposable
 * interface (defined in goog.disposable.IDisposable).
 * @constructor
 * @implements {goog.disposable.IDisposable}
 */
@:jsRequire('goog.Disposable')
extern class Disposable implements IDisposable {
  function new();

/**
 * @return {!Array<!goog.Disposable>} All {@code goog.Disposable} objects that
 *     haven't been disposed of.
 */
  static function getUndisposedObjects(): Array<Disposable>;


/**
 * Clears the registry of undisposed objects but doesn't dispose of them.
 */
  static function clearUndisposedObjects(): Void;

/**
 * If monitoring the goog.Disposable instances is enabled, stores the creation
 * stack trace of the Disposable instance.
 * @const {string}
 */
  var creationStack: String;

/**
 * @return {boolean} Whether the object has been disposed of.
 * @override
 */
  function isDisposed(): Bool;

/**
 * Disposes of the object. If the object hasn't already been disposed of, calls
 * {@link #disposeInternal}. Classes that extend {@code goog.Disposable} should
 * override {@link #disposeInternal} in order to delete references to COM
 * objects, DOM nodes, and other disposable objects. Reentrant.
 *
 * @return {void} Nothing.
 * @override
 */
  function dispose(): Void;

/**
 * Associates a disposable object with this object so that they will be disposed
 * together.
 * @param {goog.disposable.IDisposable} disposable that will be disposed when
 *     this object is disposed.
 */
  function registerDisposable(disposable: IDisposable): Void;


/**
 * Invokes a callback function when this object is disposed. Callbacks are
 * invoked in the order in which they were added. If a callback is added to
 * an already disposed Disposable, it will be called immediately.
 * @param {function(this:T):?} callback The callback function.
 * @param {T=} opt_scope An optional scope to call the callback in.
 * @template T
 */
  function addOnDisposeCallback(callback: Function, ?opt_scope: Dynamic): Void;


/**
 * Deletes or nulls out any references to COM objects, DOM nodes, or other
 * disposable objects. Classes that extend {@code goog.Disposable} should
 * override this method.
 * Not reentrant. To avoid calling it twice, it must only be called from the
 * subclass' {@code disposeInternal} method. Everywhere else the public
 * {@code dispose} method must be used.
 * For example:
 * <pre>
 *   mypackage.MyClass = function() {
 *     mypackage.MyClass.base(this, 'constructor');
 *     // Constructor logic specific to MyClass.
 *     ...
 *   };
 *   goog.inherits(mypackage.MyClass, goog.Disposable);
 *
 *   mypackage.MyClass.prototype.disposeInternal = function() {
 *     // Dispose logic specific to MyClass.
 *     ...
 *     // Call superclass's disposeInternal at the end of the subclass's, like
 *     // in C++, to avoid hard-to-catch issues.
 *     mypackage.MyClass.base(this, 'disposeInternal');
 *   };
 * </pre>
 * @protected
 */
  function disposeInternal(): Void;

/**
 * Returns True if we can verify the object is disposed.
 * Calls {@code isDisposed} on the argument if it supports it.  If obj
 * is not an object with an isDisposed() method, return false.
 * @param {*} obj The object to investigate.
 * @return {boolean} True if we can verify the object is disposed.
 */
  static function isDisposed(obj: Dynamic): Bool;
}
