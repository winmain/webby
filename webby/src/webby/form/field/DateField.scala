package webby.form.field
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoField, TemporalAccessor}

import com.fasterxml.jackson.databind.JsonNode
import webby.commons.text.DateFormats
import webby.form.{Invalid, Valid, ValidationResult}
import webby.html.{StdHtmlView, StdInputTag}

/**
  * Поле даты со стандартным шаблоном dd_mm_yyyy (его можно переопределить)
  */
class DateField(val id: String) extends ValueField[LocalDate] {self =>
  var minDate: Option[LocalDate] = None
  var maxDate: Option[LocalDate] = None

  // ------------------------------- Reading data & js properties -------------------------------
  class JsProps extends BaseJsProps {
    val nullValue = self.nullValue
    val minDate = self.minDate.map(DateFormats.yyyy_mm_dd)
    val maxDate = self.maxDate.map(DateFormats.yyyy_mm_dd)
  }
  override def jsProps = new JsProps
  override def jsField: String = "date"
  override def parseJsValue(node: JsonNode): Either[String, LocalDate] = parseJsString(node) {v =>
    try Right(parse(v.trim))
    catch {case e: Exception => Left("Некорректная дата")}
  }
  override def toJsValue(v: LocalDate): AnyRef = if (v == null) null else formatter.format(v)

  override def nullValue: LocalDate = null

  def formatter: DateTimeFormatter = DateFormats.dd_mm_yyyy_formatter
  def parse(v: String): LocalDate = LocalDate.parse(v.trim, formatter)
  def placeholder: String = "дд.мм.гггг"

  // ------------------------------- Builder & validations -------------------------------

  def minDate(v: LocalDate): this.type = {minDate = Some(v); this}
  def maxDate(v: LocalDate): this.type = {maxDate = Some(v); this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minDate.exists(get.compareTo(_) < 0)) Invalid("Не ранее " + formatter.format(minDate.get))
    else if (maxDate.exists(get.compareTo(_) > 0)) Invalid("Не позднее " + formatter.format(maxDate.get))
    else Valid
  }

  // ------------------------------- Html helpers -------------------------------

  protected def inputClass = "date"
  // Тип этого инпута - телефон, иначе при вводе даты на андроиде будет показана нативный (неудобный и медленный) инпут
  def input(implicit view: StdHtmlView): StdInputTag = view.inputTel.id(id).name(name).cls(inputClass).placeholder(placeholder)
}

/**
  * Поле месяц-год с шаблоном mm_yyyy
  */
class MonthYearField(id: String) extends DateField(id) {
  override def jsField: String = "monthYear"
  override def formatter: DateTimeFormatter = DateFormats.mm_yyyy_formatter
  override def parse(v: String): LocalDate = {
    val ta: TemporalAccessor = formatter.parse(v)
    LocalDate.of(ta.get(ChronoField.YEAR), ta.get(ChronoField.MONTH_OF_YEAR), 1)
  }
  override def placeholder: String = "мм.гггг"
  override protected def inputClass: String = "month-year"
}
