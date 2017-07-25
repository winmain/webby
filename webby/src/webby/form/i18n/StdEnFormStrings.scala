package webby.form.i18n
import webby.commons.text.{EnPlural, Plural, RusPlural}

class StdEnFormStrings extends FormStrings {
  // ------------------------------- Form -------------------------------

  override def recordPlural: Plural = EnPlural("record", "records")
  override def recordRPlural: Plural = recordPlural

  // ------------------------------- Field texts -------------------------------

  override def invalidValue: String = "Invalid value"
  override def invalidDate: String = "Invalid date"
  override def invalidEmail: String = "Invalid email"
  override def invalidUrl: String = "Invalid url"
  override def enterIntegerNumber: String = "Please enter integer number"
  override def enterRealNumber: String = "Please enter real number"

  override def noLessThanError(minValue: Any): String = "No less than " + minValue
  override def noMoreThanError(maxValue: Any): String = "No more than " + maxValue
  override def noLessThanCharsError(minValue: Int): String = "No less than " + minValue + " characters"
  override def noMoreThanCharsError(maxValue: Int): String = "No more than " + maxValue + " characters"
  override def notEarlierThanError(min: String): String = "Not earlier than " + min
  override def noLaterThanError(max: String): String = "No later than " + max
  override def invalidSymbols(symbols: String): String = "Invalid symbols: \"" + symbols + "\""
  override def urlMustContainDomain(domain: String): String = "Url must contain a domain " + domain
  override def urlMustContainOneOfDomains(domains: String): String = "Url must contain one of this domains: " + domains

  override def storageServerIsUnavailable: String = "File server is unavailable. Please try again later."

  // ------------------------------- Field placeholders -------------------------------

  override def datePlaceholder: String = "dd.mm.yyyy"
  override def monthYearPlaceholder: String = "mm.yyyy"
}
