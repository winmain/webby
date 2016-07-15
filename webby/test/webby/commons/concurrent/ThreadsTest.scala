package webby.commons.concurrent

import org.scalatest.{FreeSpec, Matchers}

import scala.util.control.Exception._

class ThreadsTest extends FreeSpec with Matchers {
  "Threads" - {
    "should restore the correct class loader" - {
      "if the block returns successfully" in {
        val currentCl = Thread.currentThread.getContextClassLoader
        Threads.withContextClassLoader(testClassLoader) {
          Thread.currentThread.getContextClassLoader shouldEqual testClassLoader
          "a string"
        } shouldEqual "a string"
        Thread.currentThread.getContextClassLoader shouldEqual currentCl
      }

      "if the block throws an exception" in {
        val currentCl = Thread.currentThread.getContextClassLoader
        (catching(classOf[RuntimeException]) opt Threads.withContextClassLoader(testClassLoader) {
          Thread.currentThread.getContextClassLoader shouldEqual testClassLoader
          throw new RuntimeException("Uh oh")
        }) shouldBe None
        Thread.currentThread.getContextClassLoader shouldEqual currentCl
      }
    }
  }
  val testClassLoader = new ClassLoader() {}
}
