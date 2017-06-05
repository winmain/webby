package webby.form

import java.util.Locale

import querio.DbTrait
import webby.commons.text.Plural
import webby.form.i18n.{FormStrings, StdRuFormStrings}

object StubForms extends BaseForms {
  override def db: DbTrait = null
  override def recordPlural: Plural = null
  override def recordRPlural: Plural = null
  override def strings(locale: Locale): FormStrings = new StdRuFormStrings
}
