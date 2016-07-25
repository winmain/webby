package webby.api.controllers

import java.nio.file.{Files, Path}

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

  /**
    * Действие выдачи содержимого файла по пути basePath + subPath.
    *
    * @param basePath Базовый путь без слеша вначале. Иначе, файл будет с абсолютным путём.
    * @param subPath  Вторая часть пути.
    */
  def at(basePath: String, subPath: String) = SimpleAction {req =>
    PageLog.noLog()
    val path: Path = StdPaths.get.root.resolve(basePath).resolve(subPath)
    if (!Files.exists(path)) {
      NotFoundRaw
    } else {
      PlainResult(HttpResponseStatus.OK, Files.readAllBytes(path))
        .withHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.forFileName(path.getFileName.toString).getOrElse(MimeTypes.BINARY))
    }
  }
}
