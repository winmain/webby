package webby.mvc

import java.io.File
import java.nio.file.{Path, Paths}

import org.apache.commons.lang3.StringUtils
import webby.api.{App, Logger}
import webby.commons.io.FileUtils
import webby.commons.system.OverridableObject

import scala.language.implicitConversions

/**
  * Базовые пути для проекта
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
    val assetsProfiles = assets / "profiles"
    def assetsProfile(profileName: String) = assetsProfiles / profileName

    val targetAssets = root / "target/assets"

    def cssAssetType = AssetType("css")
    def jsAssetType = AssetType("js")
    // Google Closure Compiled js, only for dev profile
    def jsGccAssetType = AssetType("js-gcc")
    def jsSimpleAssetType = AssetType("js-simple")

    // ------------------------------- Convenience java.nio.file.Paths methods -------------------------------

    def get(first: String, more: String*): Path = Paths.get(first, more: _*)
  }

  override protected def default: Value = new Value

  /**
    * Тип ассетов: css, js, js-simple.
    * Удобен как enum при генерации путей к каталогам ассетов.
    */
  case class AssetType(name: String) {
    def assetsPath = StdPaths.get.assets.resolve(name)
    def targetAssetsPath = StdPaths.get.targetAssets.resolve(name)
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
    val haxeBin: Path = readHaxePropPath("haxe.bin")

    /** Path to haxe std library */
    val haxeStd: Path = readHaxePropPath("haxe.std")

    /** Haxe `cp` source directories */
    val haxeCp: Vector[Path] = readHaxeProp("haxe.cp", Vector.empty, StringUtils.split(_, ':').map(Paths.get(_))(collection.breakOut))
  }
}
