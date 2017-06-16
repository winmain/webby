package js.lib;

class ArrayUtils {
  /*
  Returns true if `array` contains element `value`.
   */
  public static function contains<T>(array: Array<T>, value: T): Bool return array.indexOf(value) != -1;

  /*
  Переключить элемент в массиве.
  Если он есть в массиве, то удалить его и вернуть `false`. Если его нет, то добавить и вернуть `true`.
   */
  public static function toggle<T>(array: Array<T>, value: T): Bool {
    if (contains(array, value)) {
      array.remove(value);
      return false;
    } else {
      array.push(value);
      return true;
    }
  }

  /** If given index `idx` is already in this `array`, returns associated value.
   *
   *  Otherwise, stores value `updateValue` with index in array and returns that value.
   */
  public inline static function getOrElseUpdate<T>(array: Array<T>, idx: Int, updateValue: T): T {
    return untyped array[idx] ? array[idx] : (array[idx] = updateValue);
  }

  /*
  Returns last element of the array
   */
  public inline static function last<T>(array: Array<T>): T return array[array.length - 1];

  /*
  Returns true if array is empty
   */
  public inline static function isEmpty(array: Array<Dynamic>): Bool return array.length == 0;

  /*
  Returns true if array is not empty
   */
  public inline static function nonEmpty(array: Array<Dynamic>): Bool return array.length > 0;

  /*
  Remove all elements from the array
   */
  public static function removeAll(array: Array<Dynamic>): Void {
    array.splice(0, array.length);
  }

  /*
  Добавить элемент в массив только если его там нет
   */
  public static function pushUnique<T>(array: Array<T>, value: T) {
    if (!contains(array, value)) {
      array.push(value);
    }
  }

  /*
  Find and return first element matches the predicate in `array`. Or null if such element not found.
   */
  public static function find<T>(array: Array<T>, predicate: T -> Bool): Null<T> {
    var i = 0;
    while (i < array.length) {
      if (predicate(array[i])) return array[i];
      i++;
    }
    return null;
  }

  /*
  Tests whether a predicate holds for some of the elements of the `array`.
  Returns `true` if the given `predicate` holds for some of the elements of the `array`, otherwise `false`.
   */
  public static function exists<T>(array: Array<T>, predicate: T -> Bool): Bool {
    var i = 0;
    while (i < array.length) {
      if (predicate(array[i])) return true;
      i++;
    }
    return false;
  }

  /*
  Tests whether a predicate holds for all elements of the `array`.
  Returns `true` if the given predicate `predicate` holds for all elements of the `array`
  or if the `array` is empty. Otherwise `false`.
   */
  public static function forall<T>(array: Array<T>, predicate: T -> Bool): Bool {
    var i = 0;
    while (i < array.length) {
      if (!predicate(array[i])) return false;
      i++;
    }
    return true;
  }

  /*
  Counts the number of elements in the `array` which satisfy a `predicate`.
   */
  public static function count<T>(array: Array<T>, predicate: T -> Bool): Int {
    var i = 0;
    var ret = 0;
    while (i < array.length) {
      if (predicate(array[i])) ret++;
      i++;
    }
    return ret;
  }

  /*
  Like original Array.splice but with additional `item` to add to `array` at position `pos`.
   */
  inline public static function splice1<T>(array: Array<T>, pos: Int, len: Int, item: T): Array<T> {
    return untyped __js__('{0}.splice({1}, {2}, {3})', array, pos, len, item);
  }
}
