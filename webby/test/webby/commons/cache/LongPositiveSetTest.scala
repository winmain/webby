package webby.commons.cache
import org.scalatest.{FunSuite, Matchers}

class LongPositiveSetTest extends FunSuite with Matchers {
  test("Basic functionality") {
    val cache = new LongPositiveSet(10)
    the[IllegalArgumentException] thrownBy cache.contains(-3) getMessage() should include("Value cannot be negative")
    cache.add(10) shouldEqual true
    cache.contains(10) shouldEqual true
    cache.add(11) shouldEqual true
    cache.add(10) shouldEqual false
    cache.contains(10) shouldEqual true
    cache.contains(12) shouldEqual false
    cache.size shouldEqual 2
  }

  test("Remove, clear, release") {
    val cache = new LongPositiveSet(10)
    cache.add(1)
    cache.add(3)
    cache.size shouldEqual 2

    cache.remove(5) shouldEqual false
    cache.size shouldEqual 2
    cache.remove(3) shouldEqual true
    cache.size shouldEqual 1

    cache.clear()
    cache.size shouldEqual 0
    cache.contains(1) shouldEqual false
    cache.contains(3) shouldEqual false

    cache.add(2)
    cache.release()
    cache.size shouldEqual 0

    cache.add(5)
    cache.size shouldEqual 1
  }

  test("Rotate") {
    val cache = new LongPositiveSet(10)
    cache.add(1)
    cache.add(2)
    cache.add(3)

    cache.rotate()
    cache.size shouldEqual 3

    cache.contains(2) shouldEqual true
    cache.add(4) shouldEqual true
    cache.size shouldEqual 4

    cache.rotate()
    cache.size shouldEqual 2
    cache.contains(1) shouldEqual false
    cache.contains(2) shouldEqual true
    cache.contains(3) shouldEqual false
    cache.contains(4) shouldEqual true

    cache.rotate()
    cache.size shouldEqual 2

    cache.rotate()
    cache.size shouldEqual 0
  }
}
