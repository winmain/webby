package webby.mvc

import java.io.File
import java.nio.file.Path

import webby.api.App
import webby.commons.io.FileUtils

import scala.language.implicitConversions

/**
  * Базовые пути для проекта
  */
trait StdPaths {
  implicit def pathWrapper(path: Path): FileUtils.PathWrapper = FileUtils.pathWrapper(path)
  implicit protected class ExtendFile(f: File) {
    def /(path: String) = new File(f, path)
  }

  val root: Path = App.app.path
  @Deprecated val rootFile: File = root.toFile

  val state = root / "state"

  val assets = if (App.isDev) root / "app/assets" else root / "assets"
  val targetAssets = root / "target/assets"
  val profileAssets = assets / ("profiles/" + App.profile.name)

  // ------------------------------- convenience java.nio.file.Paths methods -------------------------------

  def get(first: String, more: String*): Path = java.nio.file.Paths.get(first, more: _*)
}

object StdPaths extends StdPaths
