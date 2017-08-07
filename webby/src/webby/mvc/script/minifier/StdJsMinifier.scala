package webby.mvc.script.minifier
import webby.commons.system.OverridableObject
import webby.mvc.script.GoogleClosureSimpleCompiler

/**
  * Стандартный минимизатор единичных js файлов.
  */
object StdJsMinifier
  extends ScriptMinifier("js-min", ".js",
    GoogleClosureSimpleCompiler.minifyEs5(_).left.map(_.map(_.toString).mkString("\n")),
    Vector("assets/js/raw", "views")) {

  /**
    * Запуск auto-minify для всех простых js-файлов специально для production.
    */
  def main(args: Array[String]) {
    minifyAll()
  }
}

object StdJsMinifierResourceHolder extends OverridableObject {
  class Value extends Base {
    def jsMinifier: ScriptMinifier = StdJsMinifier
    def jsMin(pathFromApp: String): ResourceHolder = new ResourceHolder(jsMinifier.load(pathFromApp))
  }

  override protected def default = new Value
}


/**
  * Минимизатор js-simple, работает только для продакшна в стадии sbt stage.
  */
object StdJsSimpleMinifierForStage extends
  ScriptMinifierForStage("js-simple", ".js",
    GoogleClosureSimpleCompiler.minifyEs5(_).left.map(_.map(_.toString).mkString("\n")),
    "assets/js-simple") {

  /**
    * Запуск auto-minify для всех js-simple файлов специально для production.
    */
  def main(args: Array[String]) {
    minifyAll()
  }
}
