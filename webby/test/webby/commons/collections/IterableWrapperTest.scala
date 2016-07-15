package webby.commons.collections

import org.scalatest.{FunSuite, Matchers}
import webby.commons.collection.IterableWrapper

class IterableWrapperTest extends FunSuite with Matchers{
  test("groupBy data") {
    val got = Iterator(
      3 -> 1,
      1 -> 1, 1 -> 2, 1 -> 3,
      2 -> 0, 2 -> 1,
      3 -> 0,
      5 -> 9)

    val result = Vector(
      3 -> Vector(3 -> 1),
      1 -> Vector(1 -> 1, 1 -> 2, 1 -> 3),
      2 -> Vector(2 -> 0, 2 -> 1),
      3 -> Vector(3 -> 0),
      5 -> Vector(5 -> 9))

    IterableWrapper.wrapIterator(got).groupBy(_._1).toVector shouldEqual result
  }

  test("test groupBy iterator") {
    // Проверка на то, что groupBy использует итеративный подход, и не берёт все записи разом
    val it: Iterator[Int] = Iterator(1, 2, 3)
    val grouped: Iterator[(Int, Vector[Int])] = IterableWrapper.wrapIterator(it).groupBy(a => a)
    grouped.next() shouldEqual 1 -> Vector(1)
    it.hasNext shouldEqual true
    it.next() shouldEqual 3
  }
}
