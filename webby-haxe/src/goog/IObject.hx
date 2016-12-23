package goog;

/*
IObject describes the "[]" operator (aka computed property accessor).
An object that declares that it implements IObject can restrict the "key" used to lookup a value,
and the "value" that is expected to be returned. This can be used to describe "map-like" objects.
As with all Objects, the key should be a Symbol, number, string or a value that reasonably
coerces to string.

@interface
@template KEY, VALUE
 */
typedef IObject<KEY, VALUE> = Dynamic<VALUE>;
