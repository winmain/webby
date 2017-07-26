package webby.commons.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.net.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import webby.api.mvc._
import webby.commons.io.codec.Base64UEF
import webby.commons.system.OverridableObject

import scala.reflect.ClassTag
import scala.util.Try

/**
  * Стандартная реализация класса, работающиего с json.
  * В одном проекте по-умолчанию используется только один instance этого класса, объявленный в одном объекте.
  */
object StdJs extends OverridableObject {
  class Value extends Base {
    val mapper: ObjectMapper = newMapper

    def newMapper: ObjectMapper = defaultMapper

    def toJson(o: AnyRef): String = mapper.writeValueAsString(o)

    /**
      * Сформировать результат для объекта, сконвертированного в json.
      */
    def result(obj: Any, jsMapper: ObjectMapper = mapper, status: HttpResponseStatus = HttpResponseStatus.OK): PlainResult =
      new PlainResult(status, mapper.writeValueAsBytes(obj))
        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")

    /**
      * BodyParser, используемый, чтобы сконвертировать request.body (пришедший в json) в конкретный класс.
      * Пример использования:
      * {{{
      *  case class Data(foo:Int, bar:String)
      *
      *  def action: Action(Js.bodyParser[Data]) {
      *    implicit request =>
      *      Ok("foo: " + request.body.foo)
      *  }
      * }}}
      */
    def bodyParser[T](implicit ct: ClassTag[T]): BodyParser[T] = new CheckedBodyParser[T] {
      def checkContentType(c: String): Boolean =
      // application/x-www-form-urlencoded здесь нужен, потому что на jquery так сделать проще.
      // Возможно, в последствии переделаем запросы jquery и уберём этот вариант отсюда.
        c == "text/json" || c == "application/json" || c == "application/x-www-form-urlencoded"

      protected def parse(rh: RequestHeader, body: Array[Byte]): Either[Result, T] =
        Right(mapper.readValue(body, ct.runtimeClass).asInstanceOf[T])
    }

    def serializeBase64(o: AnyRef, mapper: ObjectMapper): String = Base64UEF.encodeToString(mapper.writeValueAsBytes(o))

    def serializeBase64(o: AnyRef): String = serializeBase64(o, mapper)

    def deserializeBase64[T](data: String, mapper: ObjectMapper)(implicit ct: ClassTag[T]): Option[T] =
      Try(mapper.readValue(Base64UEF.decodeFast(data), ct.runtimeClass.asInstanceOf[Class[T]])).toOption

    def deserializeBase64[T](data: String)(implicit ct: ClassTag[T]): Option[T] = deserializeBase64(data, mapper)
  }

  override protected def default = new Value

  /**
    * Default [[ObjectMapper]], useful for tests.
    */
  def defaultMapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
}
