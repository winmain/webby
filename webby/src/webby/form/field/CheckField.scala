package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.form.Form

/**
  * Чекбокс
  */
class CheckField(val form: Form, val shortId: String, makeUniqueInputId: Boolean = false) extends ValueField[Boolean] {

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "check"
  override def parseJsValue(node: JsonNode): Either[String, Boolean] = Right(node.asBoolean())

  override def nullValue: Boolean = false
}

object CheckField {
  private var uniqueIdCounter = 0
  private[field] def nextUniqueIdCounter: Int = {uniqueIdCounter += 1; uniqueIdCounter}
}
