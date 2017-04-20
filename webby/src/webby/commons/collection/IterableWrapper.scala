package webby.commons.collection

import scala.collection.immutable.{IntMap, LongMap}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

object IterableWrapper {
  implicit def wrapIterable[A](it: Iterable[A]): IterableWrapper[A] = new IterableWrapper(it)
  implicit def wrapIterator[A](it: Iterator[A]): IteratorWrapper[A] = new IteratorWrapper(it)
  implicit def wrapArray[A](array: Array[A]): ArrayWrapper[A] = new ArrayWrapper[A](array)


  /**
   * Расширение Iterable trait
   */
  class IterableWrapper[A](iterable: Iterable[A]) {

    def foreachWithSep(f: A => Any, sep: => Unit) {
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) f(it.next())
      while (it.hasNext) {
        sep
        f(it.next())
      }
    }

    def foreachWithIndex(f: (A, Int) => Any): Unit = {
      val it: Iterator[A] = iterable.iterator
      var idx = 0
      while (it.hasNext) {
        f(it.next(), idx)
        idx += 1
      }
    }

    def mapMkString(f: A => String, sep: String): String = {
      val sb = new java.lang.StringBuilder()
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) sb append f(it.next())
      while (it.hasNext) sb append sep append f(it.next())
      sb.toString
    }

    def mapMkStringCapitalized(f: A => String, sep: String): String = {
      val sb = new java.lang.StringBuilder()
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) sb append f(it.next()).capitalize
      while (it.hasNext) sb append sep append f(it.next())
      sb.toString
    }

    def mapMkString(f: A => String, start: String, sep: String, end: String): String = {
      val sb = new java.lang.StringBuilder()
      sb append start
      val it: Iterator[A] = iterable.iterator
      if (it.hasNext) sb append f(it.next())
      while (it.hasNext) sb append sep append f(it.next())
      sb append end
      sb.toString
    }

    def mapToSet[B](f: A => B): Set[B] = {
      val b = Set.newBuilder[B]
      b.sizeHint(iterable.size)
      for (x <- iterable) b += f(x)
      b.result()
    }

    def mapToMap[K, V](f: A => (K, V)): Map[K, V] = {
      val b = Map.newBuilder[K, V]
      b.sizeHint(iterable.size)
      for (x <- iterable) b += f(x)
      b.result()
    }

    def mapToIntMap[V](f: A => (Int, V)): IntMap[V] = {
      var b = IntMap[V]()
      for (x <- iterable) b += f(x)
      b
    }

    def mapToLongMap[V](f: A => (Long, V)): LongMap[V] = {
      var b = LongMap[V]()
      for (x <- iterable) b += f(x)
      b
    }

    def toIntMap[V](implicit ev: A <:< (Int, V)): IntMap[V] = {
      var b = IntMap[V]()
      for (x <- iterable) b += x
      b
    }

    def toLongMap[V](implicit ev: A <:< (Long, V)): LongMap[V] = {
      var b = LongMap[V]()
      for (x <- iterable) b += x
      b
    }

    def mapFind[B](f: A => B, filter: B => Boolean): Option[B] = {
      val it: Iterator[A] = iterable.iterator
      while (it.hasNext) {
        val v: B = f(it.next())
        if (filter(v)) return Some(v)
      }
      None
    }

    /**
      * Работает аналогично стандартному методу [[scala.collection.TraversableLike.groupBy()]], но с отличиями:
      * 1. Позволяет сделать map значений перед записью их в результат.
      * 2. Работает быстрее оригинального groupBy, потому что не делает дополнительный проход
      * преобразования mutable.Map => immutable.Map.
      * 3. Возвращает мутабельные коллекции.
      */
    def groupMapBy[K, V](groupByFn: A => K, mapFn: A => V): mutable.Map[K, ArrayBuffer[V]] = {
      val m = mutable.Map.empty[K, ArrayBuffer[V]]
      for (elem <- iterable) {
        val key = groupByFn(elem)
        val bldr = m.getOrElseUpdate(key, ArrayBuffer())
        bldr += mapFn(elem)
      }
      m
    }
  }

  /**
   * Расширение Iterator trait
   */
  class IteratorWrapper[A](it: Iterator[A]) {wrapper =>

    def foreachWithSep[U](f: A => U, sep: => Unit) {
      if (it.hasNext) f(it.next())
      while (it.hasNext) {
        sep
        f(it.next())
      }
    }

    /**
     * Создать итератор с группировкой по заданному полю. Чтобы группировка работала корректно,
     * записи должны быть сортированы по этому полю.
     * Пример:
     * {{{
     *  Vector(1,2,1).iterator.groupBy(a=>a).toVector
     *  = Vector((1,Vector(1)), (2,Vector(2)), (1,Vector(1)))
     *
     *  Vector(1,1,2).iterator.groupBy(a=>a).toVector
     *  = Vector((1,Vector(1, 1)), (2,Vector(2)))
     * }}}
     */
    def groupBy[K](keyFn: A => K): Iterator[(K, Vector[A])] = new Iterator[(K, Vector[A])] with CurrentIteratorTrait[A] {
      override protected def it: Iterator[A] = wrapper.it

      override def hasNext: Boolean = hasCur
      override def next(): (K, Vector[A]) = {
        val key = keyFn(cur)
        val vb = Vector.newBuilder[A]
        while (hasCur && keyFn(cur) == key) {
          vb += cur
          advance()
        }
        (key, vb.result())
      }
    }
  }

  /**
   * Расширение Array trait
   */
  class ArrayWrapper[A](array: Array[A]) {

    def mapToSet[B](f: A => B): Set[B] = {
      val b = Set.newBuilder[B]
      b.sizeHint(array.length)
      for (x <- array) b += f(x)
      b.result()
    }
  }
}
