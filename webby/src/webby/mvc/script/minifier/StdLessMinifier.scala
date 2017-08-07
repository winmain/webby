package webby.mvc.script.minifier

import java.nio.file.Paths

import webby.api.{App, Profile}
import webby.commons.system.OverridableObject
import webby.mvc.script.compiler.ExternalLessCssoCompiler

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
    * Run auto-minify for all simple `less` files specially for production.
    */
  def main(args: Array[String]) {
    new StdLessMinifier(Profile.Prod).minifier.minifyAll()
  }
}


/**
  * This class used to minify resources.
  */
object StdLessMinifierResourceHolder extends OverridableObject {
  class Value extends Base {
    def makeLessMinifier: ScriptMinifier = new StdLessMinifier(App.profile).minifier
    val lessMinifier: ScriptMinifier = makeLessMinifier
    def lessMin(pathFromApp: String): ResourceHolder = new ResourceHolder(lessMinifier.load(pathFromApp))
  }

  override protected def default = new Value
}
