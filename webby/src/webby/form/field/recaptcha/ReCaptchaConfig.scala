package webby.form.field.recaptcha
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import webby.html.{CommonTag, StdHtmlView}

/**
  * Google ReCaptcha config.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "org.apache.httpcomponents" % "httpclient" % "4.5.3"
  * }}}
  *
  * @param publicKey ReCaptcha public key
  * @param secretKey ReCaptcha secret key
  */
class ReCaptchaConfig(val publicKey: String,
                      val secretKey: String) {

  /** Адрес скрипта для подключения рекапчи. TODO: проверить синхронность / async */
  def scriptUrl = "//www.google.com/recaptcha/api.js?hl=ru"

  def formParamName = "g-recaptcha-response"

  def initHttpClient = HttpClients.custom.setConnectionManager(new PoolingHttpClientConnectionManager).build

  /**
    * Div для вставки рекапчи в форму.
    */
  def divHtml(implicit view: StdHtmlView): CommonTag = view.div.cls("g-recaptcha").attr("data-sitekey", publicKey)
}
