package webby.form.html

import webby.form.Form
import webby.form.field._
import webby.html._
import webby.html.elements.{RichSelectConfig, RichSelectHtml}

/**
  * Form html helpers
  */
class StdFormHtml(protected val form: Form)(implicit protected val view: StdHtmlView, page: WebbyPage) {

  // ------------------------------- Form and layout -------------------------------

  def formTag(method: String = "post"): StdFormTag = {
    form.base.formTag(page.scripts, form, form.htmlId, method)
    // Warning! If you want to change form id, see Form.htmlId field.
  }

  def group: CommonTag = view.div.cls(form.base.formGroupCls)
  def row: CommonTag = view.div.cls(form.base.formRowCls)
  def formErrorsBlock: CommonTag = view.div.cls(form.base.formErrorsBlockCls)

  // ------------------------------- Fields -------------------------------

  def label(field: Field[_]): StdLabelTag = view.label.forId(field.htmlId).cls(form.base.fieldLabelCls)
  def labelSimple(field: Field[_]): StdLabelTag = view.label.forId(field.htmlId)

  def wrapField[T <: CommonTag with NamedTag](field: Field[_], tag: T): T = {
    tag.id(field.htmlId).name(field.name).cls(form.base.fieldCls)
  }

  def wrapFieldPH[T <: CommonTag with NamedTag with PlaceholderTag](field: PlaceholderField[_], tag: T): T = {
    val t = wrapField(field, tag)
    if (field.placeholder != null) t.placeholder(field.placeholder)
    t
  }

  def inputNumber(field: BaseIntField): StdInputTag = wrapFieldPH(field, view.inputNumber)
  def inputText(field: BaseIntField): StdInputTag = wrapFieldPH(field, view.inputText)

  def inputNumber(field: BaseLongField): StdInputTag = wrapFieldPH(field, view.inputNumber)
  def inputText(field: BaseLongField): StdInputTag = wrapFieldPH(field, view.inputText)

  def inputNumber(field: FloatField): StdInputTag = wrapFieldPH(field, view.inputNumber)
  def inputText(field: FloatField): StdInputTag = wrapFieldPH(field, view.inputText)

  def inputText(field: TextField): StdInputTag = wrapFieldPH(field, view.inputText)
  def textarea(field: TextField): StdTextareaTag = wrapFieldPH(field, view.textarea)

  def inputText(field: UrlField): StdInputTag = wrapFieldPH(field, view.inputText)

  def inputText(field: MaskedField): StdInputTag = wrapFieldPH(field, view.inputText)

  def inputText(field: EmailField): StdInputTag = wrapFieldPH(field, view.inputText)

  def inputPassword(field: PasswordField): StdInputTag = wrapFieldPH(field, view.inputPassword)

  def inputTextAutocompleteOff(field: ValueField[_] with PlaceholderField[_]): StdInputTag = wrapFieldPH(field, view.inputText).autocompleteOff

  def inputText(field: AutocompleteField[_]): StdInputTag = inputTextAutocompleteOff(field)

  def inputText(field: AutocompleteTextField): StdInputTag = inputTextAutocompleteOff(field)

  def inputHidden(field: HiddenField): StdInputTag = wrapField(field, view.inputHidden)
  def inputHidden(field: HiddenIntField): StdInputTag = wrapField(field, view.inputHidden)
  def inputHidden(field: HiddenBooleanField): StdInputTag = wrapField(field, view.inputHidden)

  // Тип этого инпута - телефон, иначе при вводе даты на андроиде будет показана нативный (неудобный и медленный) инпут
  def inputText(field: RuDateField): StdInputTag = wrapFieldPH(field, view.inputTel).cls(form.base.dateFieldCls)
  def inputText(field: RuMonthYearField): StdInputTag = wrapFieldPH(field, view.inputTel).cls(form.base.monthYearFieldCls)

  def inputTextInDiv(field: AutocompleteListField[_], itemsTag: CommonTag => CommonTag = a => a, inputTag: StdInputTag => StdInputTag = a => a): HtmlBase = {
    view.div.cls(form.base.autocompleteListFieldCls) {
      itemsTag(view.div.cls("ac-items clearfix"))
      inputTag(wrapFieldPH(field, view.inputText).attr("autocomplete", "off"))
    }
  }

  // ------------------------------- Checkboxes -------------------------------

  def inputCheckboxLabelLeft(field: CheckField): StdLabelTag = {
    wrapField(field, view.inputCheckbox)
    view.label.forId(field.htmlId).cls(form.base.checkboxLeftCls)
  }

  def inputCheckboxLabelLeft2(field: CheckField, checkBox: StdInputCheckedTag => StdInputCheckedTag): StdLabelTag = {
    checkBox(wrapField(field, view.inputCheckbox))
    view.label.forId(field.htmlId).cls(form.base.checkboxLeftCls)
  }

  def inputHidden(field: CheckField): StdInputTag = wrapField(field, view.inputHidden)

  // ------------------------------- Radio & select fields -------------------------------

  def richSelect[T](field: RichSelectField[T],
                    outerSpan: CommonTag => CommonTag = a => a,
                    selectConfig: RichSelectConfig = null): HtmlBase = {
    val selectConf = if (selectConfig != null) selectConfig else form.base.selectConfig
    RichSelectHtml(selectConf)
      .outerSpan(span => outerSpan(span.id(field.htmlId).cls(form.base.fieldCls)))
      .innerSelect(_.name(field.name))
      .render {
        field.emptyTitle.foreach(title => view.option.value("") ~ title)
        for (item <- field.items) {
          val v = field.valueFn(item)
          view.option.valueSafe(v) ~ field.titleFn(item)
        }
      }(view)
  }

  // ------------------------------- CheckListField -------------------------------

  def checkboxList[T](field: CheckListField[T], tag: String = "div", rowWrapper: StdHtmlView => CommonTag = _.div)(implicit view: StdHtmlView): HtmlBase = {
    // TODO: вынести css классы отсюда
    view.tag(tag).id(field.htmlId).cls(form.base.fieldCls).cls("check-list-field") {
      for {item <- field.items
           value = field.valueFn(item)
      } {
        rowWrapper(view) {
          val htmlId = field.htmlId + "-" + value
          view.inputCheckbox.id(htmlId).valueSafe(value)
          view.label.forId(htmlId).cls(form.base.checkboxLeftCls) ~ field.titleFn(item)
          if (field.commentFn != null) {
            val comment: String = field.commentFn(item)
            if (comment != null && !comment.isEmpty) view.div.cls("checkbox-comment") ~ comment
          }
        }
      }
    }
  }
}
