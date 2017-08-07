package webby.mvc.script.minifier

import java.nio.file.Paths

import io.bit3.jsass.OutputStyle
import webby.api.{App, Profile}
import webby.commons.system.OverridableObject
import webby.mvc.script.compiler.LibSassCompiler

/**
  * Standard minimizer for single `sass` files in views.
  */
class StdSassMinifier(profile: Profile) {
  private val sassCompiler = StdSassMinifier.sassCompiler(profile)
  private val sourcePath = Paths.get("app/assets/css/main.css")

  val minifier = new ScriptMinifier("css-min", ".sass",
    minifier = {code =>
      sassCompiler.compile(code, sourcePath)
    },
    Vector("views"))
}


object StdSassMinifier {
  def sassCompiler(profile: Profile) = LibSassCompiler(includePaths = Seq("app/assets/profiles/" + profile.toString), outputStyle = OutputStyle.COMPRESSED)

  /**
    * Run auto-minify for all simple `less` files specially for production.
    */
  def main(args: Array[String]) {
    new StdSassMinifier(Profile.Prod).minifier.minifyAll()
  }
}


object StdSassMinifierForStage extends
  ScriptMinifierForStage("css", null,
    code => StdSassMinifier.sassCompiler(Profile.Prod).compile(code, Paths.get("app/assets/css/main.css")),
    "app/assets/css") {

  /**
    * @param args For example: ["main.sass", "adm.sass", "mobile.sass"]
    */
  def main(args: Array[String]): Unit = {
    require(args.nonEmpty, "No SASS files to minify specified")
    minifyFiles(args, "css")
  }
}


/**
  * This class used to minify resources.
  */
object StdSassMinifierResourceHolder extends OverridableObject {
  class Value extends Base {
    def makeSassMinifier: ScriptMinifier = new StdSassMinifier(App.profile).minifier
    val sassMinifier: ScriptMinifier = makeSassMinifier
    def sassMin(pathFromApp: String): ResourceHolder = new ResourceHolder(sassMinifier.load(pathFromApp))
  }

  override protected def default = new Value
}
