package webby.commons.text
import scala.annotation.switch

trait Plural {
  def form(num: Int): String

  def form(num: Long): String = form((num % 100L).toInt)

  class Builder(num: Int) {
    private var numStr: String = null
    private var delimiter: String = " "

    def nbsp: Builder = {
      delimiter = "&nbsp;"
      this
    }

    def bigNumber(thousandSeparator: String): Builder = {
      numStr = StdStr.bigNumber(num, thousandSeparator)
      this
    }

    def bigNumberS: Builder = {
      numStr = StdStr.bigNumberS(num)
      this
    }

    override def toString: String = (if (numStr == null) String.valueOf(num) else numStr) + delimiter + form(num)
    def str: String = toString
  }

  def apply(num: Int): Builder = new Builder(num)
}

/**
  * Класс, описывающий 3 формы множественного числа для Русского языка.
  *
  * Примеры:
  * {{{
  * val days = RusPlural("день", "дня", "дней")
  * val months = RusPlural("месяц", "месяца", "месяцев")
  * val years = RusPlural("год", "года", "лет")
  *
  * println(days(4).str)
  * }}}
  * @param form1 Первая форма - единственное число, например: день
  * @param form2 Вторая форма - число для 2-4 единиц, например: дня
  * @param form3 Третья форма - число для 5+ единиц, например: дней
  */
case class RusPlural(form1: String, form2: String, form3: String) extends Plural {
  /**
    * Вернуть форму фразы для заданного числа. Возвращается просто фраза, без самого числа.
    */
  override def form(num: Int): String = {
    val hund = num % 100
    if (hund > 9 && hund < 21) form3
    else
      (hund % 10: @switch) match {
        case 1 => form1
        case 2 | 3 | 4 => form2
        case _ => form3
      }
  }
}
