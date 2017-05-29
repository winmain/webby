package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import querio.{MutableTableRecord, Table, TableRecord}
import webby.commons.text.StdCharMatchers
import webby.commons.text.StringWrapper.wrapper
import webby.form._

import scala.reflect.ClassTag

/**
  * Поле для ввода пароля.
  * Пароль внутри этого поля хранится в виде хеша (т.е., оригинальное значение пароля получить невозможно).
  * Методы set устанавливают хеш пароля, но не сам пароль. Для установки пароля служит метод setPassword
  */
class PasswordField(val form: Form, val shortId: String, val hashFn: String => String) extends ValueField[String] with PlaceholderField[String] {self =>
  var minLength: Option[Int] = Some(3)
  var maxLength: Option[Int] = Some(50)

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val minLength = self.minLength
    val maxLength = self.maxLength
  }
  override def jsProps = new JsProps
  override def jsField: String = "text"
  override def parseJsValue(node: JsonNode): Either[String, String] = {
    val v = node.asText()
    if (v == null || v.isEmpty) Right(nullValue)
    else Right(v)
  }
  override def nullValue: String = null

  override def toJsValue(v: String): AnyRef = null

  def getHashedPassword: String = if (isEmpty) null else hashFn(get)

  // ------------------------------- Builder & validations -------------------------------

  // Переопределим коннекторы к БД, чтобы пароль записывался в базу хешем (хеш получается через метод getHashedPassword).
  override def connect[TR <: TableRecord, MTR <: MutableTableRecord[TR]](dbField: Table[TR, MTR]#Field[_, String])(implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector[TR, MTR](new DbPasswordFieldConnector[TR, MTR](self, dbField))
  override def connect[TR <: TableRecord, MTR <: MutableTableRecord[TR]](dbField: Table[TR, MTR]#Field[_, Option[String]])(implicit form: FormWithDb[TR, MTR], ct: ClassTag[TR]): this.type =
    sys.error("Not implemented")


  def minLength(v: Int): this.type = {minLength = Some(v); this}
  def maxLength(v: Int): this.type = {maxLength = Some(v); this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minLength.exists(get.length < _)) Invalid(form.strings.noLessThanCharsError(minLength.get))
    else if (maxLength.exists(get.length > _)) Invalid(form.strings.noMoreThanCharsError(maxLength.get))
    else if (StdCharMatchers.rusLetters.matchesAnyOf(get)) Invalid("Пароль не должен содержать русских букв")
    else if (!StdCharMatchers.passwordMatcher.matchesAllOf(get))
      Invalid(form.strings.invalidSymbols(StdCharMatchers.passwordMatcher.negate.retainFrom(get).escapeXml))
    else Valid
  }
}
