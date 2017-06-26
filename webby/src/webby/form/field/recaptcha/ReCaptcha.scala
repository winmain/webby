package webby.form.field.recaptcha
import webby.api.mvc.RequestHeader

trait ReCaptcha {

  def config: ReCaptchaConfig

  /**
    * Отправить решение капчи запросом в гугл. Результат говорит о том, прошла ли проверка капчей,
    * или нет.
    * Внимание! Это синхронный метод, поэтому, его рекомендуется обрамлять примерно так:
    * {{{
    *   AsyncResult(Future {
    *     val success = RePatcha.solve(reCaptchaResponse)
    *     if (success) ... else ...
    *   })
    * }}}
    * @param reCaptchaResponse Ответ js-капчи в форме. Обычно он передаётся параметром формы.
    * @return Решение верно?
    */
  def solve(reCaptchaResponse: String)(implicit req: RequestHeader): Boolean
}
