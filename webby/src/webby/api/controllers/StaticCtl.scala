package webby.api.controllers

import java.io.InputStream
import java.nio.file.{Files, Path}

import com.google.common.io.ByteStreams
import com.google.common.net.HttpHeaders
import io.netty.handler.codec.http.HttpResponseStatus
import webby.api.libs.MimeTypes
import webby.api.mvc._
import webby.commons.io.Resources
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
    val base: Path = StdPaths.get.root.resolve(basePath).toAbsolutePath
    val path: Path = base.resolve(subPath).toAbsolutePath
    if (!path.startsWith(base)) {
      BadRequest("Invalid path")
    } else if (Files.isDirectory(path) || !Files.exists(path)) {
      NotFoundRaw
    } else {
      filePlainResult(Files.readAllBytes(path), path.getFileName.toString)
    }
  }

  /**
    * Server resource which can be loaded from classpath, from file, and with optional restriction.
    *
    * @param subPath          Relative path to resource.
    * @param resourceBasePath Base path to load resource from classpath if not null. Resulting path = resourceBasePath + subPath
    * @param fileBasePath     Base path to load resource from file if not null. Resulting path = fileBasePath + subPath
    * @param restriction      Resource restriction mechanism.
    */
  def atResource(subPath: String,
                 resourceBasePath: String = null,
                 fileBasePath: String = null,
                 restriction: ResourceRestriction = NoRestriction) = SimpleAction {req =>
    val valid: Boolean =
      restriction match {
        case NoRestriction => true // always allowed
        case CookieRestriction(cookieName, value) =>
          if (value == null) req.cookies.contains(cookieName)
          else req.cookies.get(cookieName).contains(value)
      }
    if (!valid) {
      Forbidden("Forbidden resource")
    } else {
      def serve(): Result = {
        if (resourceBasePath != null) {
          val stream: InputStream = Resources.classLoader.getResourceAsStream(resourceBasePath + subPath)
          if (stream != null) {
            return filePlainResult(ByteStreams.toByteArray(stream), subPath)
          }
        }
        if (fileBasePath != null) {
          at(fileBasePath, subPath)(req, null)
        } else {
          NotFoundRaw("Not found")
        }
      }
      serve()
    }
  }

  sealed trait ResourceRestriction
  object NoRestriction extends ResourceRestriction
  case class CookieRestriction(cookieName: String, value: String = null) extends ResourceRestriction

  // ------------------------------- Private & protected methods -------------------------------

  private def filePlainResult(body: Array[Byte], fileName: String): PlainResult =
    PlainResult(HttpResponseStatus.OK, body)
      .withHeader(HttpHeaders.CONTENT_TYPE, MimeTypes.forFileName(fileName).getOrElse(MimeTypes.BINARY))
      .withHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
}
