package goog;

/*
IArrayLike extends IObject, such that the "key" is number and the "value" is the content type
of the Array-like structure. IArrayLike is an improvement on the {length:number} type that is
traditionally used to signal array like in that the content type of the object can be supplied.

@interface
@extends {IObject<number, VALUE>}
@template VALUE
 */
typedef IArrayLike<VALUE> = {
  var length(default, never): Int;
}
