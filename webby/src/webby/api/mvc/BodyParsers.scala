package webby.api.mvc

/**
 * Default body parsers.
 */
object BodyParsers {

  sealed trait textParser extends CheckedBodyParser[String] {
    def checkContentType(c: String): Boolean = c == "text/plain"
    protected def parse(rh: RequestHeader, body: Array[Byte]): Either[Result, String] =
      try Right(new String(body, rh.contentType.charset))
      catch {
        case e: Throwable => Left(Results.BadRequest)
      }
  }

  /**
   * Parse the body as text without checking the Content-Type.
   */
  object tolerantText extends textParser {
    override def checkContentType(c: String): Boolean = true
  }

  /**
   * Parse the body as text if the Content-Type is text/plain.
   */
  object text extends textParser


  // -- Raw parser

  object bytes extends BodyParser[Array[Byte]] {
    def apply(rh: RequestHeader, body: Array[Byte]): Either[Result, Array[Byte]] = Right(body)
  }

  // -- Empty parser

  /**
   * Don't parse the body content.
   */
  object empty extends BodyParser[Unit] {
    def apply(rh: RequestHeader, body: Array[Byte]): Either[Result, Unit] = Right(())
  }

  /**
   * Parse the body as form url encoded if the Content-Type is application/x-www-form-urlencoded.
   */
  object formUrlEncoded extends CheckedBodyParser[UrlEncoded] {
    def checkContentType(c: String): Boolean = c == "application/x-www-form-urlencoded"
    protected def parse(rh: RequestHeader, body: Array[Byte]): Either[Result, UrlEncoded] = {
      Right(UrlEncoded.fromQuery(new String(body, rh.contentType.charset)))
    }
  }

}
