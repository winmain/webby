package webby.mvc.script.minifier

import java.nio.file.Paths

import io.bit3.jsass.OutputStyle
import webby.api.{App, Profile}
import webby.commons.system.OverridableObject
import webby.mvc.{AppStub, StdPaths}
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
  def sassCompiler(profile: Profile) =
    LibSassCompiler(includePaths = Seq(StdPaths.get.profile(profile.toString).toString),
      outputStyle = OutputStyle.COMPRESSED)

  /**
    * Run auto-minify for all simple `less` files specially for production.
    */
  def main(args: Array[String]) {
    require(args.length == 1, "Must be one arg: profile name")
    val profile = Profile.fromString(args(0)).getOrElse(sys.error("Invalid profile: " + args(0)))

    new StdSassMinifier(profile).minifier.minifyAllOrFail()
  }
}


object StdSassMinifierForStage {
  /**
    * @param args For example: ["prod", "main.sass", "adm.sass", "mobile.sass"]
    */
  def main(args: Array[String]): Unit = {
    require(args.length > 1, "Must be at least two args: <profile_name>, <sass_file>, ...<more_sass_files>")
    val profile = Profile.fromString(args(0)).getOrElse(sys.error("Invalid profile: " + args(0)))

    AppStub.withAppNoPluginsDev {
      new ScriptMinifierForStage("css", null,
        code => StdSassMinifier.sassCompiler(profile).compile(code, Paths.get("app/assets/css/main.css")),
        "app/assets/css")
        .minifyFilesOrFail(args.drop(1), "css")
    }
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
