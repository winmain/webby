package webby.form
import querio._
import webby.commons.io.StdJs
import webby.commons.text.Plural
import webby.form.field.Field
import webby.html._

abstract class BaseForms {self =>
  def db: DbTrait

  def recordPlural: Plural
  def recordRPlural: Plural

  def maybeChangedFieldsDao: Option[ChangedFieldsDao] = None

  def js = StdJs.get

  /** @see [[webby.form.Form.jsConfig]]*/
  def jsConfig: String = null

  // ------------------------------- Form traits -------------------------------

  protected trait BaseCommon extends Form {
    override type B = self.type
    override def base: B = self
    override def jsConfig: String = self.jsConfig
  }

  protected trait BaseWithDb[TR <: TableRecord, MTR <: MutableTableRecord[TR]] extends FormWithDb[TR, MTR] with BaseCommon

  // ------------------------------- Html helpers -------------------------------

  def formCls = "form"
  def hiddenCls = "hidden"
  def hideFormByDefault = true

  def formGroupCls = "form-group"
  def formErrorsBlockCls = "form-errors-block"
  def formRowCls = "form-row"

  def fieldCls = "field"
  def fieldLabelCls = "field-label"

  def autocompleteListFieldCls = "autocomplete-list-field"
  def dateFieldCls = "date-field"
  def monthYearFieldCls = "month-year-field"
  def checkboxLeftCls = "checkbox-left"

  def formCreateInitJsCode(form: Form): String = "Form.create(" + js.toJson(form.jsProps) + ").init()"

  def formTag(scripts: JsCodeAppender, form: Form, id: String, method: String)(implicit view: HtmlBase): StdFormTag = {
    scripts.addCode(formCreateInitJsCode(form))
    view.form.cls(formCls).clsIf(hideFormByDefault, hiddenCls).id(id).method(method)
  }

  def makeFieldHtmlId(field: Field[_]): String = field.form.htmlId + "-" + field.shortId
}


trait ChangedItemType


trait ChangedFieldsDao {
  case class ChangedFieldValue(fieldTitle: String, path: String, value: String)

  def query(tpe: ChangedItemType, item: TableRecord): Vector[ChangedFieldValue]

  // ------------------------------- Modification methods -------------------------------

  def insert(tpe: ChangedItemType, itemId: Int, field: AnyTable#ThisField, id: Int)(implicit tr: Transaction)

  def delete(tpe: ChangedItemType, itemId: Int)(implicit tr: Transaction): Unit
}
