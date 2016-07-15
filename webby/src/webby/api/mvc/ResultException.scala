package webby.api.mvc
import scala.util.control.ControlThrowable

/**
 * Специальный тип исключения, которое просто возвращает результат методом #getResult.
 * Само исключение в лог не пишется.
 */
trait ResultException extends ControlThrowable {

  def getResult(rh: RequestHeader): Result

  def raise: Nothing = throw this
}

object ResultException {
  def apply(result: Result) = new ResultException {
    def getResult(request: RequestHeader): Result = result
  }

  def apply(result: RequestHeader => Result) = new ResultException {
    def getResult(request: RequestHeader): Result = result(request)
  }

  /**
   * Расширение ResultException - кроме возврата результата, нужно сохранить полученную ошибку в лог.
   * Но. Если cause является ResultException, то его мы и вернём.
   */
  def wrap(cause: Throwable)(result: Result): ResultException = cause match {
    case e: ResultException => e
    case c => new ResultException {
      override def getCause: Throwable = c
      def getResult(request: RequestHeader): Result = result
    }
  }
}
