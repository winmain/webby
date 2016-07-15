package webby.commons.collection

import java.{util, lang => jl}

import scala.collection.immutable
import scala.language.implicitConversions

object CollectionUtils {

  def mapToVector[T, R](list: util.List[T], transform: T => R): Vector[R] = {
    val builder = immutable.Vector.newBuilder[R]
    var i = 0
    val size = list.size
    builder.sizeHint(size)
    while (i < size) {
      builder += transform(list.get(i))
      i += 1
    }
    builder.result()
  }

  @annotation.tailrec
  def first[A, B](as: Traversable[A], f: A => Option[B]): Option[B] =
    if (as.isEmpty) None
    else f(as.head) match {
      case s@Some(_) => s
      case _ => first(as.tail, f)
    }

  /**
   * Мост между scala Iterable[Int] => java Collection[Integer]
   */
  class IntegerCollection(wrapped: Iterable[Int]) extends util.AbstractCollection[jl.Integer] {
    def iterator(): util.Iterator[jl.Integer] = new util.Iterator[jl.Integer] {
      private val it = wrapped.iterator

      def hasNext: Boolean = it.hasNext

      def next(): Integer = it.next()

      def remove() {
        throw new UnsupportedOperationException
      }

    }

    def size(): Int = wrapped.size
  }

  implicit def integerCollection(iterable: Iterable[Int]): util.Collection[jl.Integer] = new IntegerCollection(iterable)
}
