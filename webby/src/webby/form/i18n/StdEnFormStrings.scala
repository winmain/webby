package webby.form.i18n

abstract class StdEnFormStrings extends FormStrings {
  // ------------------------------- Form -------------------------------

  // ------------------------------- Field texts -------------------------------

  override def noLessThanError(minValue: Any): String = "No less than " + minValue
  override def noMoreThanError(maxValue: Any): String = "No more than " + maxValue
  override def noLessThanCharsError(minValue: Int): String = "No less than " + minValue + " characters"
  override def noMoreThanCharsError(maxValue: Int): String = "No more than " + maxValue + " characters"
  override def enterIntegerNumber: String = "Please enter integer number"
  override def invalidValue: String = "Invalid value"
  override def invalidDate: String = "Invalid date"
}
