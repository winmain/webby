package webby.mvc.script.compiler

import java.nio.file.Path

/**
  * Внешний jade компилятор для js-шаблонов.
  * Умеет создавать специальные closure-модули, если исходник был аннотирован комментарием "//- module.name".
  * Использует скрипт script/jade-closure.py, который и делает эту замену.
  *
  * Для работы нужно установить jade во внутренний репозиторий npm:
  * ./npm install jade
  */
case class ExternalJadeClosureCompiler(noDebug: Boolean = false,
                                       pretty: Boolean = false) extends ScriptCompiler {
  def sourceFileExt: String = "jade"
  def targetFileExt: String = "js"
  def targetContentType: String = "application/x-javascript"

  val flags = (if (noDebug) Seq("--no-debug") else Nil) ++
    (if (pretty) Seq("--pretty") else Nil)

  override def compile(source: String, sourcePath: Path): Either[String, String] = {
    runCommonProcess(Seq("script/jade-closure.py") ++ flags, source)
  }
}
