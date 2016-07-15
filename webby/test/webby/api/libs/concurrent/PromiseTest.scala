package webby.api.libs.concurrent
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global

class PromiseTest extends WordSpec with Matchers {

  "A Promise" should {
    "recover after an exception using recover" in {
      val promise = Promise[Int]()
      promise.redeem(6 / 0)

      promise.future.recover {case e: ArithmeticException => 0}
        .value1.get shouldEqual 0
    }


    "filter Redeemed values" in {
      val p = Promise.timeout(42, 100)
      p.filter(_ == 42).value1.get shouldEqual 42
    }

    "filter Redeemed values not matching the predicate" in {
      val p = Promise.timeout(42, 100)
      a[NoSuchElementException] should be thrownBy p.filter(_ != 42).value1.get
    }

    "filter Thrown values" in {
      val p = Promise.timeout(42, 100).map[Int] {_ => throw new Exception("foo")}
      the[Exception] thrownBy p.filter(_ => true).value1.get should have message "foo"
    }
  }
}
