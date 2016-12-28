package webby.form.jsrule
import com.fasterxml.jackson.annotation.{JsonAutoDetect, JsonProperty}
import webby.form.field.Field

import scala.collection.mutable

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
  isGetterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE,
  creatorVisibility = JsonAutoDetect.Visibility.NONE,
  fieldVisibility = JsonAutoDetect.Visibility.NONE)
trait JsCondition {
  @JsonProperty def cls: String
  def check: Boolean

  def &&(other: JsCondition): JsCondition = And(this, other)
  def ||(other: JsCondition): JsCondition = Or(this, other)
  def unary_! : JsCondition = Not(this)
}

/**
  * Условие not $cond
  */
case class Not(@JsonProperty cond: JsCondition) extends JsCondition {
  override def cls: String = "not"
  override def check: Boolean = !cond.check
}

/**
  * Условие И: $c1 && $c2
  */
case class And(@JsonProperty c1: JsCondition, @JsonProperty c2: JsCondition) extends JsCondition {
  override def cls: String = "and"
  override def check: Boolean = c1.check && c2.check
}

/**
  * Условие Или: $c1 || $c2
  */
case class Or(@JsonProperty c1: JsCondition, @JsonProperty c2: JsCondition) extends JsCondition {
  override def cls: String = "or"
  override def check: Boolean = c1.check || c2.check
}

/**
  * Условие выполняется, если указанное поле имеет заданное значение
  *
  * @param f Поле
  * @param v Сравниваемое значение
  */
case class FieldEquals[T](f: Field[T], private val v: T) extends JsCondition {
  override def cls: String = "fieldEquals"
  override def check: Boolean = f.get == v
  @JsonProperty def field: String = f.id
  @JsonProperty val value: AnyRef = f.toJsValue(v)
}

/**
  * Условие выполняется, если указанное поле имеет одно из заданных значений
  *
  * @param f Поле
  * @param v Список значений для поля
  */
case class FieldIn[T](f: Field[T], private val v: Iterable[T]) extends JsCondition {
  override def cls: String = "fieldIn"
  override def check: Boolean = {val fValue = f.get; v.exists(fValue == _)}
  @JsonProperty def field: String = f.id
  @JsonProperty val values: mutable.Buffer[AnyRef] = v.map(f.toJsValue(_))(scala.collection.breakOut)
}

/**
  * Условие выполняется, если указанное поле пусто (не забыть проверить работоспособность метода isEmpty у поля)
  *
  * @param f Поле
  */
case class FieldEmpty(f: Field[_]) extends JsCondition {
  override def cls: String = "fieldEmpty"
  override def check: Boolean = f.isEmpty
  @JsonProperty def field: String = f.id
}

/**
  * Условие выполняется, если указанное поле целиком совпадает с регуляркой
  *
  * @param f     Поле
  * @param regex Регулярка для совпадения
  */
case class FieldRegex(f: Field[_], @JsonProperty regex: String) extends JsCondition {
  override def cls: String = "fieldRegex"
  override def check: Boolean = f.get.toString.matches(regex)
  @JsonProperty def field: String = f.id
}
