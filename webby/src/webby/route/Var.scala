package webby.route

// -------- vars --------
abstract class Var[R](val name: String) {
  def pattern: String
  def fromString(s: String): R
}

class IntVar(name: String, pat: Option[String]) extends Var[Int](name) {
  override val pattern: String = pat.getOrElse("-?[0-9]+")
  override def fromString(s: String): Int = Integer.parseInt(s)
  override def toString: String = "IntVar(" + pat.getOrElse("") + ")"
}

class LongVar(name: String, pat: Option[String]) extends Var[Long](name) {
  override val pattern: String = pat.getOrElse("-?[0-9]+")
  override def fromString(s: String): Long = java.lang.Long.parseLong(s)
  override def toString: String = "LongVar(" + pat.getOrElse("") + ")"
}

class StringVar(name: String, pat: Option[String]) extends Var[String](name) {
  override val pattern: String = pat.getOrElse("[^/]*")
  override def fromString(s: String): String = s
  override def toString: String = "StringVar(" + pat.getOrElse("") + ")"
}

class BigDecimalVar(name: String, pat: Option[String]) extends Var[BigDecimal](name) {
  override val pattern: String = pat.getOrElse("-?[0-9\\.]+")
  override def fromString(s: String): BigDecimal = BigDecimal(s)
  override def toString: String = "BigDecimalVar(" + pat.getOrElse("") + ")"
}
