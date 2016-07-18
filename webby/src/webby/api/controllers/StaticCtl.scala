package webby.api.controllers

import java.nio.file.Files

import com.google.common.net.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import webby.api.libs.MimeTypes
import webby.api.mvc._
import webby.commons.system.log.PageLog
import webby.mvc.{StdCtl, StdPaths}

/**
  * Простенький класс для хостинга статики в DEV режиме.
  * Стандартный объект Assets не подходит, т.к. он не умеет хостить произвольную статику без добавления её в classpath
  */
object StaticCtl extends StdCtl {

  def at(basePath: String, subPath: String) = SimpleAction {req =>
    PageLog.noLog()
    val path = StdPaths.get.root.resolve(basePath).resolve(subPath)
    if (!Files.exists(path)) {
      NotFoundRaw
    } else {
      PlainResult(HttpResponseStatus.OK, Files.readAllBytes(path))
        .withHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.forFileName(path.getFileName.toString).getOrElse(MimeTypes.BINARY))
    }
  }
}
