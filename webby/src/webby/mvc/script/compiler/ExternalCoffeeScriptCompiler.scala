package webby.mvc.script.compiler
import java.nio.file.Path

/**
  * Внешний coffee-script closure компилятор.
  *
  * Для работы нужно склонировать репозиторий https://github.com/winmain/coffee-script
  * И сделать симлинк файла в этом проекте с bin/coffee-closure на /usr/local/bin/coffee-closure
  *
  * Если требуется поддержка google closure, то нужно поставить эту реализацию coffee-script'а https://github.com/hleumas/coffee-script
  * И компилировать с ключом goog = true.
  */
case class ExternalCoffeeScriptCompiler(bare: Boolean = false,
                                        goog: Boolean = false) extends ScriptCompiler {
  def sourceFileExt: String = "coffee"
  def targetFileExt: String = "js"
  def targetContentType: String = "application/x-javascript"
  override def sourceMapFileExt: Option[String] = Some("map")

  def runBinary = "script/coffee-script-closure/bin/coffee-closure"

  val flags = (if (bare) Seq("--bare") else Nil) ++
    (if (goog) Seq("-g") else Nil)


  override def compile(source: String, sourcePath: Path): Either[String, String] = {
    runCommonProcess(Seq(runBinary, "--stdio", "--compile", "--print") ++ flags, source)
  }
}
