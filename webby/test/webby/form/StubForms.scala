package webby.form

import java.util.Locale

import querio.DbTrait
import webby.form.i18n.{FormStrings, StdEnFormStrings}

class StubForms extends BaseForms {
  override def db: DbTrait = null
  override def strings(locale: Locale): FormStrings = new StdEnFormStrings

  trait Common extends BaseCommon {
    override def initLocale: Locale = Locale.ENGLISH
  }
}

object StubForms extends StubForms
