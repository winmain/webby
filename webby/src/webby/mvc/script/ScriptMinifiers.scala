package webby.mvc.script

import java.nio.file.{Path, Paths}

import compiler.ExternalLessCssoCompiler
import webby.api.Profile

/**
  * Стандартный минимизатор единичных js файлов.
  */
object StdJsMinifier
  extends ScriptMinifier("js-min", ".js",
    GoogleClosureSimpleCompiler.minify(_).left.map(_.map(_.toString).mkString("\n")),
    Vector("assets/js/raw", "views")) {

  /**
    * Запуск auto-minify для всех простых js-файлов специально для production.
    */
  def main(args: Array[String]) {
    minifyAll()
  }
}


/**
  * Минимизатор js-simple, работает только для продакшна в стадии sbt stage.
  */
object StdJsSimpleMinifierForStage extends
  ScriptMinifier("js-simple", ".js",
    GoogleClosureSimpleCompiler.minify(_).left.map(_.map(_.toString).mkString("\n")),
    Vector("assets/js-simple")) {

  override protected def baseTargetDir: String = "target/assets-release"
  override protected def makeTargetPath(pathFromApp: String, subDir: String): Path = baseTargetPath

  /**
    * Запуск auto-minify для всех js-simple файлов специально для production.
    */
  def main(args: Array[String]) {
    minifyAll()
  }
}


/**
  * Стандартный минимизатор и оптимизатор единичных less файлов. Использует less + csso.
  */
class StdLessMinifier(profile: Profile) {
  private val lessCssoCompiler = ExternalLessCssoCompiler(includePaths = Seq("app/assets/profiles/" + profile.toString))
  private val sourcePath = Paths.get("app/assets/css/main.css")

  val minifier = new ScriptMinifier("css-min", ".less",
    minifier = {code =>
      lessCssoCompiler.compile(code, sourcePath)
    },
    Vector("views"))
}

object StdLessMinifier {
  /**
    * Запуск auto-minify для всех простых less-файлов специально для production.
    */
  def main(args: Array[String]) {
    new StdLessMinifier(Profile.Prod).minifier.minifyAll()
  }
}
