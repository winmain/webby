package lib.form.field
import com.fasterxml.jackson.databind.JsonNode
import lib.html._
import webby.commons.text.html.{StdInputCheckedTag, StdInputTag, StdLabelTag}

/**
 * Чекбокс
  *
  * @param makeUniqueInputId Если установлен этот флаг, то id для этого элемента каждый раз будет новым.
  *                          Это решает проблему с уникальностью id элемента на странице, когда
  *                          на странице есть несколько форм одного типа, то с одинаковыми id чекбоксов,
  *                          при клике на label чекбокса, всегда будешь попадать на самый первый чекбокс
  *                          самой первой формы.
 */
class CheckField(override val name: String, makeUniqueInputId: Boolean = false) extends ValueField[Boolean] {
  override val id: String =
    if (makeUniqueInputId) name + "-" + CheckField.nextUniqueIdCounter.toString
    else name

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "check"
  override def parseJsValue(node: JsonNode): Either[String, Boolean] = Right(node.asBoolean())

  override def nullValue: Boolean = false

  // ------------------------------- Html helpers -------------------------------

  def inputCheckboxLabelLeft(implicit view: HtmlView): StdLabelTag = {
    view.inputCheckbox.id(id).name(name)
    view.label.forId(id).cls("checkbox-left")
  }

  def inputCheckboxLabelLeft2(checkBox: StdInputCheckedTag => StdInputCheckedTag)(implicit view: HtmlView): StdLabelTag = {
    checkBox(view.inputCheckbox.id(id).name(name))
    view.label.forId(id).cls("checkbox-left")
  }

  def inputHidden(implicit view: HtmlView): StdInputTag = {
    view.inputHidden.id(id).name(name)
  }
}

object CheckField {
  private var uniqueIdCounter = 0
  private[field] def nextUniqueIdCounter: Int = {uniqueIdCounter += 1; uniqueIdCounter}
}
