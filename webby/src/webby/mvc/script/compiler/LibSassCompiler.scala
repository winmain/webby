package webby.mvc.script.compiler
import java.io.File
import java.nio.file.Path
import java.util

import io.bit3.jsass.{CompilationException, Compiler, Options}

import scala.collection.JavaConverters._

/**
  * Compiler for SASS files. Uses libsass library via jsass.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "io.bit3" % "jsass" % "5.5.3"
  * }}}
  */
case class LibSassCompiler(includePaths: Seq[String] = Nil) extends ScriptCompiler {
  def sourceFileExt: String = "sass"
  def targetFileExt: String = "css"
  def targetContentType: String = "text/css"

  override def compile(source: String, sourcePath: Path): Either[String, String] = {
    val compiler = new Compiler
    val options = new Options
    val sourcePaths = new util.ArrayList[File]()
    sourcePaths.addAll(includePaths.map(new File(_)).asJavaCollection)
    options.setIncludePaths(sourcePaths)

    options.setIsIndentedSyntaxSrc(true)

    try {
      Right(compiler.compileString(source, sourcePath.toUri, null, options).getCss)
    } catch {
      case e: CompilationException =>
        Left(e.getErrorMessage)
    }
  }
}
