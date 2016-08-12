package webby.commons.cache

import javax.annotation.concurrent.NotThreadSafe

import com.carrotsearch.hppc.LongHashSet
import com.carrotsearch.hppc.procedures.LongProcedure

/**
  * Набор (set), основанный на [[LongHashSet]], поддерживающий ротацию для внутренней очистки от старых
  * данных (старыми считаются записи, которые были прочитаны или записаны до последней ротации).
  * Его преимущества перед обычным Set[Long] и Guava cache в том, что он потребляет
  * значительно (нужно замерить) меньше памяти, и быстрее работает.
  * Его недостаток - он не может хранить отрицательные значения, и его самоочистка происходит
  * специфическим образом: см. [[rotate()]]
  *
  * Отрицательные значения в этом наборе используются для пометки записей удаление.
  * Внешний пользователь никогда не получит отрицательное значение из набора.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "com.carrotsearch" % "hppc" % "0.7.1" % "optional"
  * }}}
  */
@NotThreadSafe
class LongPositiveSet(expectedElementCount: Int) {
  private var set = new LongHashSet(expectedElementCount)

  def contains(v: Long): Boolean = {
    if (set.contains(safeValue(v))) true
    else {
      if (set.contains(~v)) {
        set.add(v)
        set.remove(~v)
        true
      } else false
    }
  }

  /** @return true - если элемента не было в наборе, false - если он уже там есть (даже если старый) */
  def add(v: Long): Boolean = {
    val removedOldValue = set.remove(~safeValue(v))
    set.add(v) & !removedOldValue
  }

  /** @return true - если элемент был в коллекции (даже если старый), false - если его там не было */
  def remove(v: Long): Boolean = set.remove(safeValue(v)) | set.remove(~v)

  def clear(): Unit = set.clear()
  def release(): Unit = set.release()

  def size: Int = set.size()

  /**
    * Произвести ротацию - очистку набора от старых записей.
    * Старые записи помечены в наборе отрицательными значениями.
    * Очистка происходит через копирование внутреннего [[LongHashSet]] но только с новыми значениями.
    * Новые значения в новой мапе оказываются уже старыми. Т.е., если значение не обновится, либо
    * не будет прочитано, то оно удалится при следующей ротации.
    * При самой первой ротации никакие записи удалены не будут, они просто будут помечены как старые.
    * Удаление записей происходит начиная со второй ротации. Примерно так же работает `logrotate`.
    */
  def rotate(): Unit = {
    val copy = new LongHashSet(set.size())
    set.forEach(new LongProcedure {
      override def apply(value: Long): Unit = {
        if (value >= 0) copy.add(~value)
      }
    })
    set = copy
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def safeValue(key: Long): Long = {
    require(key >= 0L, "Value cannot be negative: " + key)
    key
  }
}
