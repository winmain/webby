package webby.mvc.script.watcher

import java.nio.file.{Path, Paths}

trait TargetFileTransform {
  def transform(sourcePath: String, remappings: Map[String, String] = null): String

  def transformToPath(sourcePath: String, remappings: Map[String, String] = null): Path = Paths.get(transform(sourcePath, remappings))
}

object TargetFileTransform {
  def apply(sourceDir: Path, targetDir: Path, extension: String): TargetFileTransform = new TargetFileTransform {

    val sourcePath: String = sourceDir.toAbsolutePath.toString
    val targetPath: String = targetDir.toAbsolutePath.toString
    val extTransform = new FileExtTransform(extension)

    def relativePath(path: String): String =
      path.substring(sourcePath.length)

    def transform(sourcePath: String, remappings: Map[String, String] = null): String = {
      val rel = relativePath(sourcePath)
      val transformed = extTransform.transform(rel)
      if (remappings != null) {
        remappings.get(transformed.substring(1)) match {
          case Some(remapped) => targetPath + '/' + remapped
          case None => targetPath + transformed
        }
      } else {
        targetPath + transformed
      }
    }
  }
}