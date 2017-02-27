package webby.form
import querio.DbTrait
import webby.commons.text.Plural

object StubForms extends BaseForms {
  override def db: DbTrait = null
  override def recordPlural: Plural = null
  override def recordRPlural: Plural = null
}
