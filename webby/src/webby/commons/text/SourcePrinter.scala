package webby.commons.text

import java.io.File
import java.nio.file.{Files, Path}

import scala.collection.mutable

class SourcePrinter {
  val sb = new java.lang.StringBuilder()

  protected var _package: String = null
  protected val _imports = mutable.SortedSet[String]()
  protected val _wildImports = mutable.Set[String]()
  protected var _indent = 0

  def getSource: String = {
    val src = new java.lang.StringBuilder(sb.length() + 256)
    if (_package != null) src append "package " append _package append "\n\n"
    (_imports ++ _wildImports.map(_ + "._")).foreach {src append "import " append _ append "\n"}
    src append "\n"
    src append sb
    src.toString
  }

  def saveToFile(file: File): Unit = saveToFile(file.toPath)
  def saveToFile(path: Path): Unit = Files.write(path, getSource.getBytes)

  def ++(sql: String): this.type = { sb append sql; this }
  def ++(sql: Char): this.type = { sb append sql; this }
  def ++(sql: Int): this.type = { sb append sql; this }
  def ++(sql: Long): this.type = { sb append sql; this }
  def nl: this.type = {
    if (isEmptyLastLine) del(_indent * 2) // Удалить предыдущую строку, целиком состоящую из пробелов
    sb append '\n'
    for (i <- 0 until _indent) sb append "  "
    this
  }
  def n(): this.type = nl

  def del(chars: Int): this.type = { sb.delete(sb.length() - chars, sb.length()); this }

  def indent: this.type = { _indent += 1; this }
  def dedent: this.type = { _indent -= 1; this }

  def block(body: => Any): this.type = blockChar(" {", "}")(body)

  def blockParen(body: => Any): this.type = blockChar("(", ")")(body)

  def blockChar(open: String, close: String)(body: => Any): this.type = {
    (this ++ open).indent.nl
    body
    if (isEmptyLastLine) {
      sb.delete(sb.length() - 2, sb.length())
      dedent
    } else dedent.nl
    ++(close).nl
  }

  def isEmptyLastLine: Boolean = sb.charAt(sb.length() - _indent * 2 - 1) == '\n'

  def pkg(setPackage: String): Unit = _package = setPackage

  def imp(definition: String): Unit = {
    val idx = definition.lastIndexOf('.')
    if (idx != -1) {
      if (definition.endsWith("._")) {
        val pkg = definition.substring(0, definition.length - 2)
        _wildImports += pkg
        // remove imports duplicating this wild import
        _imports --= _imports.filter(s => s.substring(0, s.lastIndexOf('.') - 1) == pkg)
      } else {
        val pkg = definition.substring(0, idx)
        if (!_wildImports.contains(pkg)) _imports += definition
      }
    }
  }
}
