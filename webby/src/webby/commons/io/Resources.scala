package webby.commons.io

import java.io.{IOException, InputStream}
import java.net.URL
import java.nio.file.{Files, Path, Paths}

import com.google.common.io.ByteStreams
import org.apache.commons.lang3.StringUtils
import webby.api.App

/**
  * Работа с локальными ресурсами.
  */
object Resources {
  def classLoader: ClassLoader = Thread.currentThread().getContextClassLoader

  def nameForClass(cls: Class[_], append: String) = {
    val name = StringUtils.removeEnd(cls.getCanonicalName, "$")
    StringUtils.replaceChars(name, '.', '/') + append
  }

  def localPathDev(path: String): Path =
    App.maybeApp.fold(Paths.get("."))(_.path).resolve(path)

  def url(path: String): URL = classLoader.getResource(path)

  def load(path: String): InputStream = {
    val stream: InputStream = classLoader.getResourceAsStream(path)
    if (stream == null) throw new IOException(s"Resource file $path not found")
    stream
  }

  def loadBytes(path: String): Array[Byte] = {
    ByteStreams.toByteArray(load(path))
  }

  /**
    * Загружает внутренний ресурс так, что для локальной версии берёт его не из classpath, а напрямую,
    * что удобно для разработки.
    */
  def loadBytesDev(baseDir: String, path: String): Array[Byte] = {
    if (App.isDev) Files.readAllBytes(localPathDev(baseDir).resolve(path))
    else loadBytes(path)
  }

  def loadBytesDevApp(path: String): Array[Byte] = loadBytesDev("app", path)
  def loadBytesDevConf(path: String): Array[Byte] = loadBytesDev("conf", path)
}
