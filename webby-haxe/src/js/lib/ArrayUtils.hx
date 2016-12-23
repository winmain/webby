package js.lib;

class ArrayUtils {
  /*
  Проверить, есть ли элемент `value` в массиве `array`.
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
  Получить последний элемент массива
   */
  public inline static function last<T>(array: Array<T>): T return array[array.length - 1];

  public inline static function isEmpty(array: Array<Dynamic>): Bool return array.length == 0;

  public inline static function nonEmpty(array: Array<Dynamic>): Bool return array.length > 0;

  /*
  Удалить все элементы из массива
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
}
