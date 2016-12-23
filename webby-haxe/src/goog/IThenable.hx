package goog;

import haxe.Constraints.Function;

/*
IThenable is used to describe Promise like objects and improves upon
the traditional "thenable" type ({then:!Function}) by allowing the result type to be known
and appropriate handle "Promise" unwrapping.
 */
typedef IThenable = {
  function then(?opt_onFulfilled: Function, ?opt_onRejected: Function): Void;
}
