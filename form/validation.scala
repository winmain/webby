package lib.form

sealed trait ValidationResult

case object Valid extends ValidationResult

case class Invalid(message: String) extends ValidationResult

trait Constraint[T] {
  def check(v: T): ValidationResult
}
