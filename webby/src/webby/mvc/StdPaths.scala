package webby.mvc

import java.io.File
import java.nio.file.Path

import webby.api.App
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

    // ------------------------------- convenience java.nio.file.Paths methods -------------------------------

    def get(first: String, more: String*): Path = java.nio.file.Paths.get(first, more: _*)
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
}
