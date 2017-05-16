package webby.form
import webby.form.field.{AutocompleteTextField, _}
import webby.html._
import webby.html.elements.{RichSelectConfig, RichSelectHtml}

/**
  * Form html helpers
  */
class StdFormHtml(form: Form)(implicit view: StdHtmlView, page: WebbyPage) {

  // ------------------------------- Form and layout -------------------------------

  def formTag(method: String = "post"): StdFormTag =
    form.base.formTag(page.scripts, form, form.htmlId, method)

  def group: CommonTag = view.div.cls(form.base.formGroupCls)
  def row: CommonTag = view.div.cls(form.base.formRowCls)
  def formErrorsBlock: CommonTag = view.div.cls(form.base.formErrorsBlockCls)

  // ------------------------------- Fields -------------------------------

  def label(field: Field[_]): StdLabelTag = view.label.forId(field.htmlId).cls(form.base.fieldLabelCls)

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

  def inputText(field: PhoneField): StdInputTag = wrapFieldPH(field, view.inputText)

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
  def inputText(field: DateField): StdInputTag = wrapField(field, view.inputTel).cls(form.base.dateFieldCls)
  def inputText(field: MonthYearField): StdInputTag = wrapField(field, view.inputTel).cls(form.base.monthYearFieldCls)

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

  protected def radioGroupRender[T](field: RadioGroupField[T], topElemCls: String, labelCls: String): HtmlBase = {
    view.div.id(field.htmlId).cls(form.base.fieldCls).cls(topElemCls) {
      val firstIdx = if (field.emptyTitle.isDefined) -1 else 0
      val lastIdx = field.items.size - 1
      def renderItem(idx: Int, elId: String, value: String, title: String): Unit = {
        view.inputRadio.id(elId).name(field.name).valueSafe(value)
        view.label.forId(elId).cls(labelCls).clsIf(idx == firstIdx, "first").clsIf(idx == lastIdx, "last") ~ title
      }
      field.emptyTitle.foreach(title => renderItem(-1, field.htmlId + "-", "", title))
      for ((item, idx) <- field.items.zipWithIndex) {
        val v = field.valueFn(item)
        val elId = field.htmlId + "-" + v
        renderItem(idx, elId, v, field.titleFn(item))
      }
    }
  }

  class RadioGroupRenderer(field: RadioGroupField[_], topElemCls: String, labelCls: String) {
    def main(topTag: CommonTag = view.div)(body: RadioGroupInnerRenderer[_] => Any): HtmlBase =
      topTag.id(field.htmlId).cls(topElemCls) < body(new RadioGroupInnerRenderer(field, labelCls))
  }

  class RadioGroupInnerRenderer[T](field: RadioGroupField[T], labelCls: String) {
    case class Item(item: T) {
      val value: String = field.valueFn(item)
      val elId: String = field.htmlId + "-" + value
      def inputRadio: StdInputCheckedTag = view.inputRadio.id(elId).name(field.name).valueSafe(value)
      def label: StdLabelTag = view.label.forId(elId).cls(labelCls)
    }
    def withItem(fieldItem: T)(body: Item => Any): Unit = body(Item(fieldItem))
    def items: Iterable[Item] = field.items.map(Item.apply)
  }

  /**
    * Стандартная полоска радио-переключателей.
    * Пример: [ A | B | C ]
    */
  // TODO: вынести css классы отсюда
  def radioStripe(field: RadioGroupField[_]): HtmlBase = radioGroupRender(field, "radio-group-stripe", "radio-left")

  /**
    * Список классических радио-переключателей.
    * Пример:
    * () A
    * () B
    * () C
    */
  // TODO: вынести css классы отсюда
  def radioList(field: RadioGroupField[_]): HtmlBase = radioGroupRender(field, "radio-group-list", "radio-left")

  /**
    * Рисование своего элемента. Это может быть полоска типа [[radioStripe()]], список [[radioList()]],
    * или вообще что-то другое.
    *
    * @param topElemCls Класс внешнего элемента, задаёт общий стиль. Например: "radio-group-list", "radio-group-stripe"
    * @param labelCls   Класс каждого внутреннего label. Обычно это "radio-label"
    */
  // TODO: вынести css классы отсюда
  def customRenderer(field: RadioGroupField[_], topElemCls: String, labelCls: String = "radio-left"): RadioGroupRenderer =
  new RadioGroupRenderer(field, topElemCls, labelCls)

  /**
    * Рисование своего списка радио-переключателей, похожего на [[radioList()]]
    */
  def customRadioList(field: RadioGroupField[_], labelCls: String = "radio-left"): RadioGroupRenderer =
    customRenderer(field, "radio-group-list", labelCls)

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

  // ------------------------------- FormListField -------------------------------

  // TODO: переделать методы для FormListField, возможно вынести в отдельный компонент

  /**
    * Шаблон подформы, который клонируется при добавлении нового элемента.
    *
    * @param tag         Обрамляющий тег шаблона. Клонируется именно этот тег.
    * @param cls         Класс обрамляющего тега.
    * @param mobileMulti Флаг, который должен быть установлен, если подформа на десктопной версии однострочная,
    *                    а на мобильной она превращается в многострочную. Добавляет дополнительные паддинги в моб. версии.
    * @param body        Тело шаблона,
    */
  def formListTemplate[F <: Form](field: FormListField[F], tag: String = "p", cls: String = null, mobileMulti: Boolean = false)(body: F => Any): HtmlBase =
    view.tag(tag).id(field.htmlId + "-template").cls("hide").cls(cls).clsIf(mobileMulti, "mobile-multi") < body(field.formStub)

  def formListBlockTemplate[F <: Form](field: FormListField[F], tag: String = "section", cls: String = "form-block subform")(body: F => Any): HtmlBase =
    view.tag(tag).id(field.htmlId + "-template").cls("hide").cls(cls) {
      formListBlockRemoveTag(field)
      body(field.formStub)
    }

  def formListPlaceholder(field: FormListField[_ <: Form]): CommonTag = view.div.id(field.htmlId + "-list")
  def formListSmallAddTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls("dotted").id(field.htmlId + "-add")
  def formListSmallRemoveTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls("small-remove").id(formListRemoveId)
  def formListBlockAddTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls("block-add").id(field.htmlId + "-add")
  def formListBlockRemoveTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls("block-remove").id(formListRemoveId).title("Удалить блок")

  /** id для html элемента удаления записи */
  def formListRemoveId: String = "remove"
}
