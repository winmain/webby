package webby.api.mvc

import webby.api._

import scala.annotation.tailrec

/**
 * An Handler handles a request.
 */
trait Handler

trait Action extends Handler {
  def apply(rh: RequestHeader, body: Array[Byte]): Result
}

object SimpleAction {
  def apply(block: RequestHeader => Result) = new Action {
    override def apply(rh: RequestHeader, body: Array[Byte]): Result = block(rh)
  }
}

object ActionTools {
  def executeWithFinalizers(block: => Result, rh: RequestHeader, finalizersProvider: => List[Result => Result]): Result = {
    @tailrec def finalize(result: Result, items: List[Result => Result]): Result = items match {
      case Nil => result
      case finalizer :: tail => finalize(finalizer(result), tail)
    }
    var result: Result = null
    try {
      result = block
    } catch {
      case e: ResultException =>
        result = e.getResult(rh)
        throw e
    } finally {
      if (result != null) result = finalize(result, finalizersProvider)
    }
    result
  }
}


/**
 * A body parser parses the HTTP request body content.
 *
 * @tparam A the body content type
 */
trait BodyParser[+A] {self =>

  def apply(rh: RequestHeader, body: Array[Byte]): Either[Result, A]

  def name: String = self.getClass.getName

  /**
   * Transform this BodyParser[A] to a BodyParser[B]
   */
  def map[B](f: A => B): BodyParser[B] = new BodyParser[B] {
    override def apply(rh: RequestHeader, body: Array[Byte]): Either[Result, B] = self(rh, body).right.map(f)
    override def toString = self.toString
  }
  override def toString = "BodyParser(" + name + ")"
}

trait CheckedBodyParser[+A] extends BodyParser[A] {
  def apply(rh: RequestHeader, body: Array[Byte]): Either[Result, A] = {
    if (checkContentType(rh.contentType.name)) {
      try parse(rh, body)
      catch {
        case e: Throwable =>
          if (App.isDevOrJenkins) Logger.logger.error("Cannot parse body", e)
          Left(App.app.global.onBadRequest(rh, "Error parsing request body"))
      }
    } else Left(App.app.global.onBadRequest(rh, "Invalid Content-Type"))
  }

  def checkContentType(c: String): Boolean
  protected def parse(rh: RequestHeader, body: Array[Byte]): Either[Result, A]
}
