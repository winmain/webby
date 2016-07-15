package compiler

import java.nio.file.Path

/**
  * Внешний less компилятор + csso оптимизатор.
  *
  * Для работы нужно установить lessc, csso во внутренний репозиторий npm:
  * ./npm install lessc csso
  */
case class ExternalLessCssoCompiler(includePaths: Seq[String] = Nil) extends ScriptCompiler {
  def sourceFileExt: String = "less"
  def targetFileExt: String = "css"
  def targetContentType: String = "text/css"

  val lesscBinary = npmScriptPath + "/lessc"
  val cssoBinary = npmScriptPath + "/csso"

  override def compile(source: String, sourcePath: Path): Either[String, String] = {
    runLessc(source, sourcePath).right.flatMap {compiledCss =>
      runCsso(compiledCss)
    }
  }

  private def runLessc(source: String, sourcePath: Path): Either[String, String] = {
    val incPaths = sourcePath.getParent.toString +: includePaths
    val flags = Seq("--include-path=" + incPaths.mkString(":"))
    val params: Seq[String] = Seq(lesscBinary) ++ flags ++ Seq("-")
    runCommonProcess(params, source)
  }

  private def runCsso(source: String): Either[String, String] = {
    runCommonProcess(Seq(cssoBinary), source)
  }
}
