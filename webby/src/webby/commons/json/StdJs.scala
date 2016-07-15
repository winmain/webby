package webby.commons.json
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.net.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import webby.api.mvc._
import webby.commons.codec.Base64UEF

import scala.reflect.ClassTag
import scala.util.Try

/**
  * Стандартная реализация класса, работающиего с json.
  * В одном проекте по-умолчанию используется только один instance этого класса, объявленный в одном объекте.
  *
  * @param setAsDefaultStdJs Этот флаг используется для того, чтобы все модули фреймворка использовали
  *                          только один instance класса [[StdJs]], объявленный в приложении.
  *                          Установите флаг в true, если в приложении используется только один стандартный объект Js.
  */
class StdJs(setAsDefaultStdJs: Boolean) {
  if (setAsDefaultStdJs) StdJs._js = this


  val mapper: ObjectMapper = newMapper

  def newMapper: ObjectMapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)

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

object StdJs {
  private[json] var _js: StdJs = _

  def js: StdJs = {
    if (_js == null) _js = new StdJs(false)
    _js
  }
}
