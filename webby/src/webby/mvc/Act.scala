package webby.mvc
import webby.api.mvc.{RequestHeader, Result}

trait ActTrait extends RequestHeader {
  def body: Array[Byte]

  def prependFinalizer(finalizer: Result => Result): Unit

  /**
    * Create and throw [[webby.api.mvc.ResultException]] if code execution need to be stopped.
    * In this case [[Act.finalizers]] will not be executed, and thrown exception will be handled on Netty server level.
    */
  def throwResult(result: => Result): Nothing
}
