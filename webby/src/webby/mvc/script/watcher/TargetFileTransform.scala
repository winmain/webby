package webby.mvc.script.watcher

import java.nio.file.{Path, Paths}

trait TargetFileTransform {
  def transform(sourcePath: String): String

  def transformToPath(sourcePath: String): Path = Paths.get(transform(sourcePath))
}

object TargetFileTransform {
  def apply(sourceDir: Path, targetDir: Path, extension: String): TargetFileTransform = new TargetFileTransform {

    val sourcePath: String = sourceDir.toAbsolutePath.toString
    val targetPath: String = targetDir.toAbsolutePath.toString
    val extTransform = new FileExtTransform(extension)

    def relativePath(path: String): String =
      path.substring(sourcePath.length)

    def transform(sourcePath: String): String = {
      val rel = relativePath(sourcePath)
      targetPath + extTransform.transform(rel)
    }
  }
}