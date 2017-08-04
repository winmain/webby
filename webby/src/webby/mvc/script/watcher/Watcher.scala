package webby.mvc.script.watcher

import java.nio.file.{Files, Path, WatchKey, WatchService}

import webby.commons.collection.Empty
import webby.commons.io.Using

import scala.collection.JavaConverters._

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
  * Наблюдатель, который подписывается на все события изменения файлов в заданных каталогах.
  * Если хотябы один из файлов меняется, то следует пересобрать все файлы.
  */
class WideWatcher(watchDirs: Seq[Path]) extends Watcher {
  def this(watchDir: Path) = this(Seq(watchDir))

  import java.nio.file.StandardWatchEventKinds._

  private var lastModified: Long = {
    def maxModifyTime(dir: Path): Long = {
      var time: Long = 0
      Using(Files.newDirectoryStream(dir))(_.asScala.foreach {path =>
        val t: Long = if (Files.isDirectory(path)) maxModifyTime(path) else Files.getLastModifiedTime(path).toMillis
        if (t > time) time = t
      })
      time
    }
    watchDirs.withFilter(Files.isDirectory(_)).map(maxModifyTime) match {
      case Empty() => 0
      case dirs => dirs.max
    }
  }

  private val watcher: WatchService = watchDirs.head.getFileSystem.newWatchService()

  private def regDir(dirPath: Path) {
    if (Files.isDirectory(dirPath)) {
      dirPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
      Using(Files.newDirectoryStream(dirPath))(_.asScala.foreach {subPath =>
        regDir(subPath)
      })
    }
  }
  watchDirs.foreach(regDir)

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
