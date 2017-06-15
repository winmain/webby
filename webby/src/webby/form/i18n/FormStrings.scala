package webby.form.i18n

trait FormStrings {
  // ------------------------------- Form -------------------------------

  // ------------------------------- Field texts -------------------------------

  def invalidValue: String
  def invalidDate: String
  def invalidEmail: String
  def invalidUrl: String
  def enterIntegerNumber: String
  def enterRealNumber: String

  def noLessThanError(minValue: Any): String
  def noMoreThanError(maxValue: Any): String
  def noLessThanCharsError(minValue: Int): String
  def noMoreThanCharsError(maxValue: Int): String
  def notEarlierThanError(min: String): String
  def noLaterThanError(max: String): String
  def invalidSymbols(symbols: String): String
  def urlMustContainDomain(domain: String): String
  def urlMustContainOneOfDomains(domains: String): String

  def storageServerIsUnavailable: String

  // ------------------------------- Field placeholders -------------------------------

  def datePlaceholder: String
  def monthYearPlaceholder: String
}
