package webby.commons.cache

import javax.annotation.concurrent.NotThreadSafe

import com.carrotsearch.hppc.IntObjectHashMap
import com.carrotsearch.hppc.procedures.IntObjectProcedure

/**
  * (Не оттестирован, нужно писать тесты).
  * Реализация похожа на [[IntIntPositiveValueMap]], только здесь отрицательное значение ключей зарезервировано.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "com.carrotsearch" % "hppc" % "0.7.1" % "optional"
  * }}}
  */
@NotThreadSafe
class IntObjectPositiveKeyMap[V <: AnyRef](expectedElementCount: Int) {
  private var map = new IntObjectHashMap[V](expectedElementCount)

  def get(key: Int, default: V): V = {
    map.get(safeKey(key)) match {
      case null =>
        map.get(~key) match {
          case null => default
          case v =>
            map.remove(~key)
            map.put(key, v)
            v
        }
      case v => v
    }
  }

  def containsKey(key: Int): Boolean = map.containsKey(safeKey(key)) || map.containsKey(~key)

  def put(key: Int, value: V) {
    map.remove(~safeKey(key))
    map.put(key, value)
  }

  def remove(key: Int) {
    map.remove(safeKey(key))
    map.remove(~key)
  }
  def clear(): Unit = map.clear()
  def release(): Unit = map.release()

  def size: Int = map.size()

  /**
    * Произвести ротацию - очистку словаря от старых записей.
    * Старые записи помечены в словаре отрицательными значениями.
    * Очистка происходит через копирование внутреннего [[IntObjectHashMap]] но только с новыми значениями.
    * Новые значения в новой мапе оказываются уже старыми. Т.е., если значение не обновится, либо
    * не будет прочитано, то оно удалится при следующей ротации.
    * При самой первой ротации никакие записи удалены не будут, они просто будут помечены как старые.
    * Удаление записей происходит начиная со второй ротации. Примерно так же работает `logrotate`.
    */
  def rotate(): Unit = {
    val copy = new IntObjectHashMap[V](map.size())
    map.forEach(new IntObjectProcedure[V] {
      override def apply(key: Int, value: V): Unit = {
        if (key >= 0) copy.put(~key, value)
      }
    })
    map = copy
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def safeKey(key: Int): Int = {
    require(key >= 0, "Key cannot be negative: " + key)
    key
  }
}
