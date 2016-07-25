package webby.mvc.script.watcher

import java.nio.file.{Files, Path, WatchKey, WatchService}

import scala.collection.JavaConversions._

trait Watcher {
  /**
    * Узнать, нужно ли перекомпилировать файл, потому что он изменился.
    */
  def pollFile(path: Path, targetPath: Path): Boolean
}

/**
  * Простой наблюдатель. Он не наблюдает за всеми файлами в каталоге, а всего-лишь сравнивает время исходного файла и созданного.
  */
object SeparateWatcher extends Watcher {
  /**
    * Узнать, нужно ли перекомпилировать файл, потому что он изменился.
    */
  def pollFile(path: Path, targetPath: Path): Boolean =
    Files.getLastModifiedTime(targetPath).toMillis < Files.getLastModifiedTime(path).toMillis
}

/**
  * Наблюдатель, который подписывается на все события изменения файлов в заданном каталоге.
  * Если хотябы один из файлов меняется, то следует пересобрать все файлы.
  */
class WideWatcher(watchDir: Path) extends Watcher {

  import java.nio.file.StandardWatchEventKinds._

  private var lastModified: Long = {
    def maxModifyTime(dir: Path): Long = {
      var time: Long = 0
      resource.managed(Files.newDirectoryStream(dir)).foreach(_.foreach {path =>
        val t: Long = if (Files.isDirectory(path)) maxModifyTime(path) else Files.getLastModifiedTime(path).toMillis
        if (t > time) time = t
      })
      time
    }
    maxModifyTime(watchDir)
  }

  private val watcher: WatchService = watchDir.getFileSystem.newWatchService()

  private def regDir(dirPath: Path) {
    dirPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    resource.managed(Files.newDirectoryStream(dirPath)).foreach(_.foreach {subPath =>
      if (Files.isDirectory(subPath))
        regDir(subPath)
    })
  }
  regDir(watchDir)

  /**
    * Узнать, нужно ли перекомпилировать файл, потому что он изменился (либо изменились рядом лежащие файлы).
    */
  def pollFile(path: Path, targetPath: Path): Boolean = {
    pollWatcher()
    Files.getLastModifiedTime(targetPath).toMillis < lastModified
  }

  private def pollWatcher() {
    val key: WatchKey = watcher.poll()
    if (key != null) {
      key.pollEvents()
      lastModified = System.currentTimeMillis()
      key.reset()
    }
  }
}