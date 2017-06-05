package webby.form.i18n

class StdRuFormStrings extends FormStrings {
  // ------------------------------- Form -------------------------------

  // ------------------------------- Field texts -------------------------------

  override def invalidValue: String = "Некорректное значение"
  override def invalidDate: String = "Некорректная дата"
  override def invalidEmail: String = "Некорректный email"
  override def invalidUrl: String = "Некорректная ссылка"
  override def enterIntegerNumber: String = "Введите целое число"
  override def enterRealNumber: String = "Введите вещественное число"

  override def noLessThanError(minValue: Any): String = "Не менее " + minValue
  override def noMoreThanError(maxValue: Any): String = "Не более " + maxValue
  override def noLessThanCharsError(minValue: Int): String = "Не менее " + minValue + " символов"
  override def noMoreThanCharsError(maxValue: Int): String = "Не более " + maxValue + " символов"
  override def notEarlierThanError(min: String): String = "Не ранее " + min
  override def noLaterThanError(max: String): String = "Не позднее " + max
  override def invalidSymbols(symbols: String): String = "Недопустимые символы: «" + symbols + "»"
  override def urlMustContainDomain(domain: String): String = "Ссылка должна содержать домен " + domain
  override def urlMustContainOneOfDomains(domains: String): String = "Ссылка должна содержать один из доменов: " + domains

  // ------------------------------- Field placeholders -------------------------------

  override def datePlaceholder: String = "дд.мм.гггг"
  override def monthYearPlaceholder: String = "мм.гггг"
}
