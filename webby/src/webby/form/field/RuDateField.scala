package webby.form.field
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, TemporalAccessor}

import com.fasterxml.jackson.databind.JsonNode
import webby.commons.text.DateFormats
import webby.form.{Form, Invalid, Valid, ValidationResult}

/**
  * Поле даты со стандартным шаблоном dd_mm_yyyy (его можно переопределить)
  */
class RuDateField(val form: Form, val shortId: String) extends ValueField[LocalDate] with PlaceholderField[LocalDate] {self =>
  var minDate: Option[LocalDate] = None
  var maxDate: Option[LocalDate] = None

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val nullValue = self.nullValue
    val minDate = self.minDate.map(formatter.format)
    val maxDate = self.maxDate.map(formatter.format)
  }
  override def jsProps = new JsProps
  override def jsField: String = "ruDate"
  override def parseJsValue(node: JsonNode): Either[String, LocalDate] = parseJsString(node) {v =>
    try Right(parse(v.trim))
    catch {case _: Exception => Left(form.strings.invalidDate)}
  }
  override def toJsValue(v: LocalDate): AnyRef = if (v == null) null else formatter.format(v)

  override def nullValue: LocalDate = null

  override protected def defaultPlaceholder = form.strings.datePlaceholder

  def formatter: DateTimeFormatter = DateFormats.dd_mm_yyyy_formatter

  def parse(v: String): LocalDate = LocalDate.parse(v.trim, formatter)

  // ------------------------------- Builder & validations -------------------------------

  def minDate(v: LocalDate): this.type = {minDate = Some(v); this}
  def maxDate(v: LocalDate): this.type = {maxDate = Some(v); this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minDate.exists(get.compareTo(_) < 0)) Invalid(form.strings.notEarlierThanError(formatter.format(minDate.get)))
    else if (maxDate.exists(get.compareTo(_) > 0)) Invalid(form.strings.noLaterThanError(formatter.format(maxDate.get)))
    else Valid
  }
}

/**
  * Поле месяц-год с шаблоном mm_yyyy
  */
class RuMonthYearField(form: Form, id: String) extends RuDateField(form, id) {
  override def jsField: String = "ruMonthYear"
  override def formatter: DateTimeFormatter = DateFormats.mm_yyyy_formatter
  override def parse(v: String): LocalDate = {
    val ta: TemporalAccessor = formatter.parse(v)
    LocalDate.of(ta.get(ChronoField.YEAR), ta.get(ChronoField.MONTH_OF_YEAR), 1)
  }
  override protected def defaultPlaceholder: String = form.strings.monthYearPlaceholder
}
