package webby.commons.cache

import javax.annotation.concurrent.NotThreadSafe

import com.carrotsearch.hppc.IntIntHashMap
import com.carrotsearch.hppc.procedures.IntIntProcedure

/**
  * Словарь (map), основанный на [[IntIntHashMap]], поддерживающий ротацию для внутренней очистки от старых
  * данных (старыми считаются записи, которые были прочитаны или записаны до последней ротации).
  * Его преимущества перед обычным Map[Int, Int] и Guava cache в том, что он потребляет
  * значительно (в 5 раз меньше чем guava) меньше памяти, и быстрее работает.
  * Его недостаток - он не может хранить отрицательные значения (values), и его самоочистка происходит
  * специфическим образом: см. [[rotate()]]
  *
  * Объём занимаемой памяти (расчёт был для 2 млн. элементов) - 17.5 байт на запись при стандартном
  * load_factor = 0.75. Для сравнения, guava cache - 91.8 байт на запись.
  *
  * Отрицательные значения в этом словаре используются для пометки записей удаление.
  * Внешний пользователь никогда не получит отрицательное значение из словаря.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "com.carrotsearch" % "hppc" % "0.7.1" % "optional"
  * }}}
  */
@NotThreadSafe
class IntIntPositiveValueMap(expectedElementCount: Int) {
  private var map = new IntIntHashMap(expectedElementCount)

  def get(key: Int, default: Int): Int = {
    map.getOrDefault(key, safeValue(default)) match {
      case v if v < 0 =>
        val value = ~v
        map.put(key, value)
        value
      case v => v
    }
  }

  def containsKey(key: Int): Boolean = map.containsKey(key)

  def put(key: Int, value: Int): Int = map.put(key, safeValue(value))
  def putIfAbsent(key: Int, value: Int): Boolean = map.putIfAbsent(key, safeValue(value))
  def putOrAdd(key: Int, putValue: Int, incrementValue: Int): Int = map.putOrAdd(key, safeValue(putValue), safeValue(incrementValue))

  def remove(key: Int): Int = map.remove(key)
  def clear(): Unit = map.clear()
  def release(): Unit = map.release()

  def size: Int = map.size()

  /**
    * Произвести ротацию - очистку словаря от старых записей.
    * Старые записи помечены в словаре отрицательными значениями.
    * Очистка происходит через копирование внутреннего [[IntIntHashMap]] но только с новыми значениями.
    * Новые значения в новой мапе оказываются уже старыми. Т.е., если значение не обновится, либо
    * не будет прочитано, то оно удалится при следующей ротации.
    * При самой первой ротации никакие записи удалены не будут, они просто будут помечены как старые.
    * Удаление записей происходит начиная со второй ротации. Примерно так же работает `logrotate`.
    */
  def rotate(): Unit = {
    val copy = new IntIntHashMap(map.size())
    map.forEach(new IntIntProcedure {
      override def apply(key: Int, value: Int): Unit = {
        if (value >= 0) copy.put(key, ~value)
      }
    })
    map = copy
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def safeValue(value: Int): Int = {
    require(value >= 0, "Value cannot be negative: " + value)
    value
  }
}
