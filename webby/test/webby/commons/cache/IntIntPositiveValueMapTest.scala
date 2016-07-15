package webby.commons.cache
import org.scalatest.{FunSuite, Matchers}

class IntIntPositiveValueMapTest extends FunSuite with Matchers {
  test("Basic functionality") {
    val cache = new IntIntPositiveValueMap(10)
    cache.get(1, 5) shouldEqual 5
    the[IllegalArgumentException] thrownBy cache.get(1, -3) getMessage() should include("Value cannot be negative")
    cache.put(10, 3) shouldEqual 0
    the[IllegalArgumentException] thrownBy cache.put(10, -2) getMessage() should include("Value cannot be negative")
    cache.put(11, 2) shouldEqual 0
    cache.put(10, 1) shouldEqual 3
    cache.containsKey(10) shouldEqual true
    cache.containsKey(12) shouldEqual false
    cache.size shouldEqual 2

    the[IllegalArgumentException] thrownBy cache.putIfAbsent(15, -3) getMessage() should include("Value cannot be negative")
    the[IllegalArgumentException] thrownBy cache.putOrAdd(15, -1, 1) getMessage() should include("Value cannot be negative")
    the[IllegalArgumentException] thrownBy cache.putOrAdd(15, 2, -1) getMessage() should include("Value cannot be negative")

    cache.putIfAbsent(15, 5) shouldEqual true
    cache.putIfAbsent(15, 0) shouldEqual false
    cache.get(15, 0) shouldEqual 5

    cache.putOrAdd(15, 9, 1) shouldEqual 6
    cache.putOrAdd(16, 9, 1) shouldEqual 9
    cache.get(15, 0) shouldEqual 6
    cache.get(16, 0) shouldEqual 9
    cache.size shouldEqual 4
  }

  test("Remove, clear, release") {
    val cache = new IntIntPositiveValueMap(10)
    cache.put(1, 5)
    cache.put(3, 4)
    cache.size shouldEqual 2

    cache.remove(5) shouldEqual 0
    cache.size shouldEqual 2
    cache.remove(3) shouldEqual 4
    cache.size shouldEqual 1

    cache.clear()
    cache.size shouldEqual 0
    cache.get(1, 99) shouldEqual 99
    cache.get(3, 99) shouldEqual 99

    cache.put(2, 5)
    cache.release()
    cache.size shouldEqual 0

    cache.put(5, 1)
    cache.size shouldEqual 1
  }

  test("Rotate") {
    val cache = new IntIntPositiveValueMap(10)
    cache.put(1, 5)
    cache.put(2, 7)
    cache.put(3, 3)

    cache.rotate()
    cache.size shouldEqual 3

    cache.get(2, 99) shouldEqual 7
    cache.put(4, 4) shouldEqual 0
    cache.size shouldEqual 4

    cache.rotate()
    cache.size shouldEqual 2
    cache.containsKey(1) shouldEqual false
    cache.get(2, 99) shouldEqual 7
    cache.containsKey(3) shouldEqual false
    cache.get(4, 99) shouldEqual 4

    cache.rotate()
    cache.size shouldEqual 2

    cache.rotate()
    cache.size shouldEqual 0
  }
}
