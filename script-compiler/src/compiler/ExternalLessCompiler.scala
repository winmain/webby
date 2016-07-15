package compiler

import java.nio.file.Path

/**
  * Внешний less компилятор.
  *
  * Для работы нужно установить lessc во внутренний репозиторий npm:
  * ./npm install lessc
  */
case class ExternalLessCompiler(includePaths: Seq[String] = Nil) extends ScriptCompiler {
  def sourceFileExt: String = "less"
  def targetFileExt: String = "css"
  def targetContentType: String = "text/css"

  val runBinary = npmScriptPath + "/lessc"

  override def compile(source: String, sourcePath: Path): Either[String, String] = {
    val incPaths = sourcePath.getParent.toString +: includePaths
    val flags = Seq("--include-path=" + incPaths.mkString(":"))
    val params: Seq[String] = Seq(runBinary) ++ flags ++ Seq("-")
    runCommonProcess(params, source)
  }
}
