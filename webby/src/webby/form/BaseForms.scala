package webby.form
import querio.{Transaction, _}
import webby.commons.io.StdJs
import webby.commons.text.Plural
import webby.html.{CommonTag, JsCodeAppender, StdFormTag, StdHtmlView}

abstract class BaseForms {self =>
  def db: DbTrait

  def recordPlural: Plural
  def recordRPlural: Plural

  def maybeChangedFieldsDao: Option[ChangedFieldsDao] = None

  def js = StdJs.get

  /** @see [[webby.form.Form.jsConfig]] */
  def jsConfig: String = null

  // ------------------------------- Form traits -------------------------------

  trait Common extends Form {
    override type B = self.type
    override def base: B = self
    override def jsConfig: String = self.jsConfig
  }

  trait WithDb[TR <: TableRecord, MTR <: MutableTableRecord[TR]] extends FormWithDb[TR, MTR] with Common

  // ------------------------------- Html helpers -------------------------------

  def formCls = "form hidden"
  def formBlockCls = "form-block"

  def formCreateInitJsCode(form: Form, id: String): String = "Form.create(Tag.find('#" + id + "'), " + js.toJson(form.jsProps) + ").init()"

  def formTag(scripts: JsCodeAppender, form: Form, id: String, method: String)(implicit view: StdHtmlView): StdFormTag = {
    scripts.addCode(formCreateInitJsCode(form, id))
    view.form.cls(formCls).id(id).method(method)
  }

  def sectionBlock(implicit view: StdHtmlView): CommonTag = view.section.cls(formBlockCls)
}


trait ChangedItemType


trait ChangedFieldsDao {
  case class ChangedFieldValue(fieldTitle: String, path: String, value: String)

  def query(tpe: ChangedItemType, item: TableRecord): Vector[ChangedFieldValue]

  // ------------------------------- Modification methods -------------------------------

  def insert(tpe: ChangedItemType, itemId: Int, field: AnyTable#ThisField, id: Int)(implicit tr: Transaction)

  def delete(tpe: ChangedItemType, itemId: Int)(implicit tr: Transaction): Unit
}
