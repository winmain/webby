package webby.form.html
import webby.form.field.CheckListField
import webby.html.{CommonTag, HtmlBase, StdHtmlView}

trait CheckListFormHtml {self: StdFormHtml =>

  // ------------------------------- CSS styles -------------------------------

  def checkListFieldCls = "check-list-field"
  def checkboxCommentCls = "checkbox-comment"

  // ------------------------------- Html methods -------------------------------

  def checkboxList[T](field: CheckListField[T], tag: String = "div", rowWrapper: StdHtmlView => CommonTag = _.div)(implicit view: StdHtmlView): HtmlBase = {
    view.tag(tag).id(field.htmlId).cls(form.base.fieldCls).cls(checkListFieldCls) {
      for {item <- field.items
           value = field.valueFn(item)
      } {
        rowWrapper(view) {
          val htmlId = field.htmlId + "-" + value
          view.inputCheckbox.id(htmlId).valueSafe(value)
          view.label.forId(htmlId).cls(form.base.checkboxLeftCls) ~ field.titleFn(item)
          if (field.commentFn != null) {
            val comment: String = field.commentFn(item)
            if (comment != null && !comment.isEmpty) view.div.cls(checkboxCommentCls) ~ comment
          }
        }
      }
    }
  }
}
