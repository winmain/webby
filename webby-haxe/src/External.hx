/*
Class for working with JSON data received from server or other external source.
 */
abstract External(Dynamic) from Dynamic to Dynamic {
  public function new(v: Dynamic) {
    this = v;
  }

  @:arrayAccess
  inline public function get(field: String): External {
    return untyped __js__('{0}[{1}]', this, field);
  }

  inline public function getOr(field: String, defaultValueFn: Void -> External): External {
    return untyped __js__('{0}[{1}] || {2}', this, field, defaultValueFn());
  }

  inline public function getT<T>(field: String): T return get(field);

  inline public function getBool(field: String): Bool return get(field);

  inline public function getInt(field: String): Int return get(field);

  inline public function getString(field: String): String return get(field);

  inline public function getFloat(field: String): Float return get(field);

  inline public function getArray(field: String): Array<External> return get(field);


  @:arrayAccess
  inline public function set(field: String, value: Dynamic) {
    untyped __js__('{0}[{1}] = {2}', this, field, value);
  }

  inline public function remove(field: String) {
    untyped __js__('delete {0}[{1}]', this, field);
  }
}
