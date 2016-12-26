@:native("Object")
extern class JMap<K, V> {

  inline static function create<K, V>(): JMap<K, V> return untyped __js__('{}');

  inline function set(key: K, value: V): V {
    return untyped this[cast key] = value;
  }

  inline function get(key: K): V {
    return untyped this[cast key];
  }

  inline function getOrElse(key: K, def: Void -> V): V {
    return G.or(get(key), def);
  }

  inline function contains(key: K): Bool {
    return untyped this[cast key];
  }

  inline function containsStrict(key: K): Bool {
    return Reflect.hasField(this, cast key);
  }

  inline function remove(key: K): Void {
    Reflect.deleteField(this, cast key);
  }

  inline function keys(): Array<K> return untyped __js__('Object.keys({0})', this);

  /**
		Returns an iterator of the Array values.
	**/
  inline function iterator(): Iterator<V> return JMapUtils.makeIterator(this);

  inline function getOrUpdate(key: K, defaultValue: V): V return JMapUtils.getOrUpdate(this, key, defaultValue);
}

class JMapUtils {
  static public function makeIterator<K, V>(map: JMap<K, V>): Iterator<V> {
    var it = map.keys().iterator();
    return untyped {
      hasNext : function() return it.hasNext(),
      next : function() return map.get(it.next())
    };
  }

  static public function getOrUpdate<K, V>(map: JMap<K, V>, key: K, defaultValue: V): V {
    var v = map.get(key);
    if (v == null) {
      map.set(key, defaultValue);
      return defaultValue;
    } else {
      return v;
    }
  }
}
