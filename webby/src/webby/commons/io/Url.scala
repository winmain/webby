package webby.commons.io

import javax.annotation.Nullable

import org.apache.commons.lang3.StringUtils
import webby.commons.text.SB

abstract class Url {
  @Nullable def protocol: String
  @Nullable def domain: String
  def path: String

  /**
    * Вернуть локальный путь в StringBuilder'е. Там уже прописан localPath. Можно добавить к нему окончание.
    */
  def pathSb: SB = pathToSb(new SB(64))

  def pathToSb(sb: SB): SB = {sb + path}

  def isLocal: Boolean = domain == null

  def localUrl: Url = if (isLocal) this else Url.local(path)
  def localUrlSb(pathSbFn: SB => SB): Url = Url.local(pathSbFn(pathSb).str)

  def urlWithProtocol(protocol: String): String = {
    require(!isLocal, "Cannot write local url with protocol (no domain present), url:" + url)
    protocol + "://" + domain + path
  }
  def urlWithHttp: String = urlWithProtocol("http")
  def urlWithHttps: String = urlWithProtocol("https")

  /**
    * Полный url, безопасный для записи в параметре тега типа <a href="{value}">.
    * В url двойная кавычка '"' заменена на %22.
    */
  def quotedUrl: String = StringUtils.replace(url, "\"", "%22")
  def quotedUrlWithProtocol(protocol: String): String = StringUtils.replace(urlWithProtocol(protocol), "\"", "%22")
  def quotedUrlWithHttp: String = quotedUrlWithProtocol("http")
  def quotedUrlWithHttps: String = quotedUrlWithProtocol("https")

  def quotedLocalUrl: String = StringUtils.replace(path, "\"", "%22")

  def copyPath(newPath: String): Url

  /** Создать новый Url такого же типа с произвольной добавкой на конце */
  def sb(fn: SB => SB): Url = copyPath(fn(pathSb).str)
  /** Создать новый Url такого же типа, у которого на конце подписан символ '?' или '&', в зависимости от содержимого урла */
  def sbAnd(fn: SB => SB): Url = copyPath(fn {
    val sb = pathSb
    val addChar: Char = if (sb.sb.indexOf("?") == -1) '?' else '&'
    sb + addChar
  }.str)

  def url: String = {
    if (isLocal) path
    else {
      val sb = new SB(64)
      if (protocol != null) sb + protocol + "://" else sb + "//"
      sb + domain
      pathToSb(sb).toString
    }
  }

  override def toString = url
}

object Url {
  def local(path: String): Url = new CommonUrl(null, null, path)
  def common(domain: String, path: String): Url = new CommonUrl(null, domain, path)
  def common(protocol: String, domain: String, path: String): Url = new CommonUrl(protocol, domain, path)
}

/**
  * Полная ссылка с доменом. Используется для создания ссылок, заполнения редиректов.
  * Как правило, только для внутренних ссылок сайта, но может использоваться и для внешних
  * (здесь небольшое препятствие в виде отдельных полей протокола, домена, и пути).
  *
  * @param protocol Протокол, например "https"
  * @param domain   Домен, например "example.com"
  * @param path     Локальный путь со слешем вначале, например "/some/path"
  */
case class CommonUrl(@Nullable protocol: String,
                     @Nullable domain: String,
                     path: String) extends Url {
  override def copyPath(newPath: String): Url = copy(path = newPath)
}
