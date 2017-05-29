package webby.form.field
import javax.annotation.Nullable

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.base.CharMatcher
import webby.commons.text.StdStrHtml
import webby.form.{Form, Invalid, Valid, ValidationResult}

/**
  * Поле ввода фамилии имени отчества
  */
class RusFioField(val form: Form, val shortId: String) extends ValueField[RusFio] with PlaceholderField[RusFio] {self =>
  /** Имя поля, в котором выбирается пол. Оно указывается, если нужно автоматически выбрать пол по отчеству */
  var sexField: String = null

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val sexField = self.sexField
  }
  override def jsProps = new JsProps
  override def jsField: String = "fio"
  override def parseJsValue(node: JsonNode): Either[String, RusFio] = {
    if (node == null || node.isNull || node.size() == 0) Right(nullValue)
    else if (node.size() < 2 || node.size() > 3) Left("Введите фамилию имя отчество")
    else {
      def trimmedCapString(node: JsonNode): String = {
        val text: String = node.asText()
        if (text == null) null
        else {
          val s = text.trim
          if (s.isEmpty) null else StdStrHtml.unescapeAndCleanHtmlEntities(s).capitalize
        }
      }

      val family = trimmedCapString(node.get(0))
      val name = trimmedCapString(node.get(1))
      if (family == null || name == null) Left(form.strings.invalidValue)
      else
        Right(RusFio(family, name, patronymic = if (node.size() == 3) trimmedCapString(node.get(2)) else null))
    }
  }
  override def toJsValue(v: RusFio): AnyRef = if (v == null) null else Seq(v.family, v.name, v.patronymic)

  override def nullValue: RusFio = null

  // ------------------------------- Builder & validations -------------------------------

  def sexField(fieldName: String): this.type = {sexField = fieldName; this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (!RusFioField.allowedChars.matchesAllOf(get.family))
      Invalid("Введите фамилию русскими буквами")
    else if (!RusFioField.allowedChars.matchesAllOf(get.name))
      Invalid("Введите имя русскими буквами")
    else if (get.patronymic != null && !RusFioField.allowedChars.matchesAllOf(get.patronymic))
      Invalid("Введите отчество русскими буквами")
    else if (get.family.length > 255 || get.name.length > 255 || (get.patronymic != null && get.patronymic.length > 255))
      Invalid("Слишком длинное поле")
    else Valid
  }
}


object RusFioField {
  private[field] val allowedChars = new CharMatcher {
    override def matches(c: Char): Boolean =
      (c >= 'а' && c <= 'я') ||
        (c >= 'А' && c <= 'Я') ||
        c == 'ё' || c == 'Ё' ||
        c == '-' || c == '\''
    override def precomputed(): CharMatcher = this
  }
}

case class RusFio(family: String, name: String, @Nullable patronymic: String)
