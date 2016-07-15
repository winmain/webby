package webby.commons.stat

import org.scalatest.{FunSuite, Matchers}
import webby.commons.stat.ActionTimeCollector._

class ActionTimeCollectorTest extends FunSuite with Matchers {
  test("test default all empty storages") {
    val collector: ActionTimeCollector = new ActionTimeCollector
    collector.actionsHourly.length shouldEqual 24
    collector.millisHourly.length shouldEqual 24
    collector.actionsHourly.forall(_ == EmptyStorage) shouldEqual true
    collector.millisHourly.forall(_ == EmptyStorage) shouldEqual true
  }

  test("test some byte storages") {
    val collector: ActionTimeCollector = new ActionTimeCollector
    collector.put(0, 0, 5, 20)
    collector.put(0, 1, 3, 252)
    collector.put(5, 59, 1, 54)
    collector.getActions(0, 0) shouldEqual 5
    collector.getMillis(0, 0) shouldEqual 20
    collector.getActions(0, 1) shouldEqual 3
    collector.getMillis(0, 1) shouldEqual 252
    collector.getActions(5, 59) shouldEqual 1
    collector.getMillis(5, 59) shouldEqual 54
    val usedHours = Vector(0, 5)
    val unusedHours = collector.actionsHourly.indices.filterNot(usedHours.contains)
    usedHours.foreach(hour => collector.actionsHourly(hour) shouldBe a[ByteStorage])
    usedHours.foreach(hour => collector.millisHourly(hour) shouldBe a[ByteStorage])
    unusedHours.foreach(hour => collector.actionsHourly(hour) shouldEqual EmptyStorage)
    unusedHours.foreach(hour => collector.millisHourly(hour) shouldEqual EmptyStorage)
  }

  test("test auto-upgrade storages") {
    val collector: ActionTimeCollector = new ActionTimeCollector
    collector.put(0, 0, 0, 20)
    collector.getMillis(0, 0) shouldEqual 20
    collector.millisHourly(0) shouldBe a[ByteStorage]

    collector.put(0, 0, 0, 5000)
    collector.getMillis(0, 0) shouldEqual 5000
    collector.millisHourly(0) shouldBe a[ShortStorage]

    collector.put(0, 0, 0, 100000)
    collector.getMillis(0, 0) shouldEqual 100000
    collector.millisHourly(0) shouldBe a[IntStorage]

    collector.put(0, 0, 0, 10000000000L)
    collector.getMillis(0, 0) shouldEqual 10000000000L
    collector.millisHourly(0) shouldBe a[LongStorage]
  }
}
