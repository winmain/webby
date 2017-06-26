package webby.form.field.recaptcha
import java.io.IOException
import java.util

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import webby.api.mvc.RequestHeader
import webby.commons.io.StdJs

import scala.annotation.tailrec
import scala.util.Random

/**
  * ReCaptcha client implementation
  */
class ReCaptchaImpl(val config: ReCaptchaConfig) extends ReCaptcha {
  protected val httpClient = config.initHttpClient


  @tailrec
  private def readUrl[A](fn: => A, remainingTries: Int = 15): A = {
    try {
      fn
    } catch {
      case e: IOException =>
        if (remainingTries <= 1) throw new RuntimeException("Cannot receive captcha response for 15 tries", e)
        Thread.sleep(Random.nextInt(500))
        readUrl(fn, remainingTries - 1)
    }
  }

  override def solve(reCaptchaResponse: String)(implicit req: RequestHeader): Boolean = {
    if (reCaptchaResponse.isEmpty) false
    else {
      val verifyResult: ReCaptchaVerify =
        readUrl({
          val post: HttpPost = new HttpPost("https://www.google.com/recaptcha/api/siteverify")
          post.setEntity(
            new UrlEncodedFormEntity(util.Arrays.asList[NameValuePair](
              new BasicNameValuePair("secret", config.secretKey),
              new BasicNameValuePair("response", reCaptchaResponse),
              new BasicNameValuePair("remoteip", req.remoteAddress))))
          val response = httpClient.execute(post)
          StdJs.get.mapper.readValue(response.getEntity.getContent, classOf[ReCaptchaVerify])
        })
      verifyResult.success
    }
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
case class ReCaptchaVerify(success: Boolean)
