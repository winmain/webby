package webby.mvc

import java.io.File
import java.nio.file.{Path, Paths}

import org.apache.commons.lang3.StringUtils
import webby.api.{App, Logger}
import webby.commons.io.FileUtils
import webby.commons.system.OverridableObject

import scala.language.implicitConversions

/**
  * Application base paths
  */
object StdPaths extends OverridableObject {
  class Value extends Base {
    implicit def pathWrapper(path: Path): FileUtils.PathWrapper = FileUtils.pathWrapper(path)
    implicit protected class ExtendFile(f: File) {
      def /(path: String) = new File(f, path)
    }

    val root: Path = App.app.path
    @Deprecated val rootFile: File = root.toFile

    val app = root / "app"

    val state = root / "state"

    val public = root / "public"

    val assets = if (App.isDev) root / "app/assets" else root / "assets"

    val profiles = root / "profiles"
    def profile(profileName: String) = profiles / profileName

    val targetAssets = root / "target/assets"

    def cssAssetType: AssetType = CommonAssetType("css")
    def jsAssetType: AssetType = CommonAssetType("js")
    // Google Closure Compiled js, only for dev profile
    def jsGccAssetType: AssetType = CommonAssetType("js-gcc")
    def jsSimpleAssetType: AssetType = CommonAssetType("js-simple")
    def jsTestAssetType: AssetType = CommonAssetType("js-test")

    // ------------------------------- Convenience java.nio.file.Paths methods -------------------------------

    def get(first: String, more: String*): Path = Paths.get(first, more: _*)
  }

  override protected def default: Value = new Value

  /**
    * Asset types: css, js, js-simple.
    * Useful as enum for resolving paths to assets.
    */
  trait AssetType {
    def name: String
    def sourcePath: Path
    def watchPath: Path
    def targetPath: Path
  }

  case class CommonAssetType(name: String) extends AssetType {
    override def sourcePath: Path = StdPaths.get.assets.resolve(name)
    override def watchPath: Path = sourcePath
    override def targetPath: Path = StdPaths.get.targetAssets.resolve(name)
  }

  case class AppWatchAssetType(name: String) extends AssetType {
    override def sourcePath: Path = StdPaths.get.assets.resolve(name)
    override def watchPath: Path = StdPaths.get.app
    override def targetPath: Path = StdPaths.get.targetAssets.resolve(name)
  }

  // ------------------------------- Haxe parameters -------------------------------

  trait HaxeValue {

    protected def readHaxeProp[T](name: String, empty: => T, prepare: String => T): T =
      System.getProperty(name) match {
        case null =>
          if (App.appOrNull != null && App.isDev) Logger.warn(s"No `$name` property defined")
          empty
        case value =>
          prepare(value)
      }
    protected def readHaxePropPath(name: String): Path = readHaxeProp[Path](name, null, Paths.get(_))

    /** Path to haxe binary */
    lazy val haxeBin: Path = readHaxePropPath("haxe.bin")

    /** Path to haxe std library */
    lazy val haxeStd: Path = readHaxePropPath("haxe.std")

    /** Haxe `cp` source directories */
    lazy val haxeCp: Vector[Path] = readHaxeProp("haxe.cp", Vector.empty, StringUtils.split(_, ':').map(Paths.get(_))(collection.breakOut))
  }

  /** For internal use in webby */
  def getHaxeValue: HaxeValue = get match {
    case v: HaxeValue => v
    case _ => sys.error("HaxeValue is not defined in StdPaths. Use for example something like `object AppPaths extends StdPaths.Value with StdPaths.HaxeValue`")
  }
}
