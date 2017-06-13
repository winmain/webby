package webby.form.jsrule
import webby.commons.io.jackson.JacksonAnnotations._
import webby.form.SubForm
import webby.form.field.{Field, FormListField}

trait JsAction extends JsonDisableAutodetect {
  @JsonProperty def cls: String
  /** Это действие выполняется только на клиенте? Т.е., нет смысла выполнять его на сервере */
  def jsOnly: Boolean
  /** Это действие выполняется только на сервере? Т.е., нет смысла передавать его клиенту. */
  def serverOnly: Boolean
  def execute(turnOn: Boolean)
}

abstract class JsFieldAction(f: Field[_]) extends JsAction {
  @JsonProperty def field: String = f.shortId
  override def jsOnly: Boolean = false
  override def serverOnly: Boolean = false
}

abstract class JsFieldServerOnlyAction(f: Field[_]) extends JsFieldAction(f) {
  override def cls: String = null
  override def serverOnly: Boolean = true
}

/**
  * Действие установки видимости/скрытия поля
  *
  * @param f          Поле
  * @param vis        Поле видимо при turnOn == true?
  * @param focus      Поле получит фокус при срабатывании этого действия (когда оно будет показано)?
  */
case class Visible(f: Field[_], @JsonProperty vis: Boolean, @JsonProperty focus: Boolean) extends JsFieldAction(f) {
  override def cls: String = "visible"
  override def jsOnly: Boolean = true
  override def execute(turnOn: Boolean): Unit = {}
}

/**
  * Действие включения/выключения поля (enable/disable)
  *
  * @param f      Поле
  * @param enable Поле должно быть включено при turnOn == true?
  * @param focus  Поле получит фокус при срабатывании этого действия (когда оно будет включено)?
  */
case class Enable(f: Field[_], @JsonProperty enable: Boolean, @JsonProperty focus: Boolean) extends JsFieldAction(f) {
  override def cls: String = "enable"
  override def jsOnly: Boolean = true
  override def execute(turnOn: Boolean): Unit = {}
}

/**
  * Действие включения/выключения обязательности поля
  *
  * @param f       Поле
  * @param require Поле обязательно для заполнения при turnOn == true?
  * @param focus   Поле получит фокус при срабатывании этого действия (когда оно станет обязательным для заполнения)?
  */
case class Require(f: Field[_], @JsonProperty require: Boolean, @JsonProperty focus: Boolean) extends JsFieldAction(f) {
  override def cls: String = "require"
  override def execute(turnOn: Boolean): Unit = f.require(require ^ !turnOn)
}

/**
  * Действие установки флага игнорирования поля
  *
  * @param f      Поле
  * @param ignore Поле будет игнорироваться при turnOn == true?
  */
case class Ignore(f: Field[_], ignore: Boolean) extends JsFieldServerOnlyAction(f) {
  override def execute(turnOn: Boolean): Unit = f.ignore(ignore ^ !turnOn)
}

/**
  * Действие установки значения поля. Срабатывает только при turnOn == true.
  *
  * @param f Поле
  * @param v Устанавливаемое значение поля
  */
case class SetValue[T](f: Field[T], v: T) extends JsFieldAction(f) {
  override def cls: String = "setValue"
  override def execute(turnOn: Boolean): Unit = if (turnOn) f.set(v)
  @JsonProperty def value: AnyRef = f.toJsValue(v)
}

/**
  * Действие установки значения поля для каждого значения turnOn (одно для turnOn==false, другое для turnOn==true).
  *
  * @param f    Поле
  * @param vOn  Устанавливаемое значение поля при turnOn == true
  * @param vOff Устанавливаемое значение поля при turnOn == false
  */
case class SetValue2[T](f: Field[T], vOn: T, vOff: T) extends JsFieldAction(f) {
  override def cls: String = "setValue2"
  override def execute(turnOn: Boolean): Unit = f.set(if (turnOn) vOn else vOff)
  @JsonProperty def valueOn: AnyRef = f.toJsValue(vOn)
  @JsonProperty def valueOff: AnyRef = f.toJsValue(vOff)
}

/**
  * Действие добавления/удаления подформы.
  *
  * @param f Поле.
  */
case class AddSubform(f: FormListField[_ <: SubForm]) extends JsFieldAction(f) {
  override def cls: String = "addSubform"
  override def jsOnly: Boolean = true
  override def execute(turnOn: Boolean): Unit = {}
}
