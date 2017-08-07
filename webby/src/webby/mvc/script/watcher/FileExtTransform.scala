package webby.mvc.script.watcher

class FileExtTransform(toExtension: String) {

  def transform(path: String): String = {
    path.lastIndexOf('.') match {
      case -1 => throw new IllegalArgumentException("Invalid path")
      case idx => path.substring(0, idx + 1) + toExtension
    }
  }
}

object AsIsFileExtTransform extends FileExtTransform(null) {
  override def transform(path: String): String = path
}
