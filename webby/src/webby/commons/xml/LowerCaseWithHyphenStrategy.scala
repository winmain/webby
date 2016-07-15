package webby.commons.xml

import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase

/**
  * Стратегия именования свойств camelCaseProperty => camel-case-property
  */
class LowerCaseWithHyphenStrategy extends PropertyNamingStrategyBase {
  override def translate(input: String): String = {
    if (input == null) return input // garbage in, garbage out
    val length: Int = input.length
    val result: StringBuilder = new StringBuilder(length * 2)
    var resultLength: Int = 0
    var wasPrevTranslated: Boolean = false
    var i = 0
    while (i < length) {
      var c: Char = input.charAt(i)
      if (i > 0 || c != '_') {
        // skip first starting underscore
        if (Character.isUpperCase(c)) {
          if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '-') {
            result.append('-')
            resultLength += 1
          }
          c = Character.toLowerCase(c)
          wasPrevTranslated = true
        } else {
          wasPrevTranslated = false
        }
        result.append(c)
        resultLength += 1
      }
      i += 1
    }
    if (resultLength > 0) result.toString() else input
  }
}
