package webby.commons.io
import java.io.File
import java.nio.file.Path

import scala.language.implicitConversions

object FileUtils {
  implicit def fileWrapper(file: File): FileWrapper = new FileWrapper(file)
  implicit def pathWrapper(path: Path): PathWrapper = new PathWrapper(path)

  class FileWrapper(file: File) {
    def /(name: String): File = new File(file, name)
  }

  class PathWrapper(path: Path) {
    def /(name: String): Path = path.resolve(name)
    def /(sub: Path): Path = path.resolve(sub)

    def name: String = path.getFileName.toString
    def nameEndsWith(suffix: String): Boolean = name.endsWith(suffix)
  }
}
