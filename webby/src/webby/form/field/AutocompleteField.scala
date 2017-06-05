package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.form.Form

/**
  * Обычный автокомплит, состоящий из одного поля типа input type=text.
  * Возможно выбрать только вариант из списка. Свободный ввод запрещён.
  */
class AutocompleteField[T](val form: Form,
                           val shortId: String,
                           var jsSourceFunction: String,
                           var jsSourceArg: Any = null,
                           var toJs: T => Int,
                           var fromJs: Int => Option[T],
                           var addRendererCls: String = null)
  extends ValueField[T] with PlaceholderField[T] {self =>

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val sourceFn = self.jsSourceFunction
    val sourceArg = self.jsSourceArg
    val addRendererCls = self.addRendererCls
  }
  override def jsProps = new JsProps
  override def jsField: String = "autocomplete"
  override def parseJsValue(node: JsonNode): Either[String, T] = parseJsInt(node) {intValue =>
    fromJs(intValue) match {
      case Some(v) => Right(v)
      case None => Left(form.strings.invalidValue)
    }
  }
  override def toJsValue(v: T): AnyRef = if (v == null) null else toJs(v).asInstanceOf[AnyRef]

  override def nullValue: T = null.asInstanceOf[T]
}
