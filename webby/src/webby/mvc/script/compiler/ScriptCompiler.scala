package webby.mvc.script.compiler
import java.io.OutputStream
import java.nio.file.{Files, Path}
import javax.annotation.Nullable

import com.google.common.base.Charsets
import webby.commons.io.IOUtils

abstract class ScriptCompiler {

  def sourceFileExt: String
  def targetFileExt: String
  def targetContentType: String
  def sourceMapFileExt: Option[String] = None

  def compile(source: String, sourcePath: Path): Either[String, String]

  def compileFile(sourcePath: Path, outputPath: Path): Either[String, Unit] = {
    val source = new String(Files.readAllBytes(sourcePath), Charsets.UTF_8)
    compile(source, sourcePath).right.map {result =>
      Files.createDirectories(outputPath.getParent)
      Files.write(outputPath, result.getBytes(Charsets.UTF_8))
      Right(())
    }
  }

  protected def runCommonProcess(command: Seq[String],
                                 @Nullable input: String = null,
                                 env: Seq[String] = Nil): Either[String, String] = {
    val proc: Process = Runtime.getRuntime.exec(command.toArray, if (env.isEmpty) null else env.toArray)

    if (input != null) {
      val os: OutputStream = proc.getOutputStream
      os.write(input.getBytes)
      os.close()
    }
    val result: String = IOUtils.readString(proc.getInputStream)
    val errors: String = IOUtils.readString(proc.getErrorStream)
    proc.waitFor()
    if (proc.exitValue() == 0 && errors.isEmpty) Right(result)
    else Left(errors)
  }

  val sourceDotExt: String = "." + sourceFileExt
  val targetDotExt: String = "." + targetFileExt
  val sourceMapDotExt: Option[String] = sourceMapFileExt.map("." + _)

  def npmScriptPath = "script/npm/node_modules/.bin"

  /**
    * Этот компилятор собирает только точки входа, и только тогда, когда они явно нужны?
    * Без этого флага, [[webby.mvc.script.GoogleClosureServer]] запускает компиляторы для всех
    * файлов с расширением [[sourceFileExt]]. А с этим флагом, компилируются только те файлы,
    * которые явно запрашиваются юзером. Обычно, это точки входа.
    */
  def lazyCompiler = false
}
