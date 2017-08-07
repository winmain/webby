package webby.mvc.script.minifier

import java.nio.file.{Files, Path, Paths}
import java.util.function.Consumer
import javax.annotation.Nullable

import com.google.common.base.Charsets
import org.apache.commons.io.FileUtils
import webby.api.App
import webby.commons.concurrent.Executors
import webby.commons.io.{IOUtils, Resources}
import webby.mvc.script.watcher.{AsIsFileExtTransform, FileExtTransform}

class ScriptMinifier(dirPrefix: String,
                     sourceFileEnding: String,
                     minifier: String => Either[String, String],
                     allowedSubDirs: Seq[String]) {
  val log = webby.api.Logger(getClass)

  protected def baseTargetDir: String = "target/asset-resources"

  protected val baseSourcePath: Path = Resources.localPathDev("app")
  protected val baseTargetPath: Path = Resources.localPathDev(baseTargetDir + "/" + dirPrefix)

  protected def makeSourcePath(pathFromApp: String): Path = baseSourcePath.resolve(pathFromApp)
  protected def makeTargetPath(pathFromApp: String): Path = baseTargetPath.resolve(pathFromApp)

  def getAllowedSubDir(pathFromApp: String): Option[String] =
    allowedSubDirs.find(subDir => pathFromApp.startsWith(subDir + "/"))

  protected def loadDev(pathFromApp: String, subDir: String): Either[String, String] = {
    val sourcePath = makeSourcePath(pathFromApp)
    val targetPath = makeTargetPath(pathFromApp)
    if (!Files.exists(sourcePath)) return Left("File not found: " + sourcePath)

    val needUpdate = !Files.exists(targetPath) || Files.getLastModifiedTime(sourcePath) != Files.getLastModifiedTime(targetPath)
    if (needUpdate) {
      log.info("Minify: " + pathFromApp)
      minifier(IOUtils.readString(sourcePath)) match {
        case Left(errors) => Left(errors)
        case Right(code) =>
          saveMinified(sourcePath, targetPath, code)
          Right(code)
      }
    } else Right(IOUtils.readString(targetPath))
  }

  // null возвращает только в случае ошибки минификации скрипта
  @Nullable def load(pathFromApp: String): String = {
    getAllowedSubDir(pathFromApp) match {
      case Some(subDir) =>
        if (App.isDev) {
          loadDev(pathFromApp, subDir) match {
            case Left(error) => log.error(error); null
            case Right(code) => code
          }
        } else {
          new String(Resources.loadBytes(dirPrefix + "/" + pathFromApp), Charsets.UTF_8)
        }
      case None =>
        // TODO: в этом случае, для локалки не происходит автоматического обновления файла
        new String(Resources.loadBytes(pathFromApp), Charsets.UTF_8)
    }
  }

  def minifyAll(): Unit = {
    println("--- Minifier step: " + dirPrefix + " ---")
    val executorThreadNum = 4
    val t0 = System.currentTimeMillis()
    Executors.withSynchronousQueueExecutor("minifier-%d", executorThreadNum) {executor =>
      def minifyDir(subDir: String): Unit = {
        val sourceDir = makeSourcePath(subDir)
        val targetDir = makeTargetPath(subDir)
        if (Files.isDirectory(sourceDir))
          Files.walk(sourceDir).forEach(new Consumer[Path] {
            override def accept(path: Path): Unit = {
              if (Files.isRegularFile(path) && path.getFileName.toString.endsWith(sourceFileEnding)) {
                executor.execute {
                  minifyFile(path, sourceDir, targetDir, AsIsFileExtTransform)
                }
              }
            }
          })
      }

      if (Files.exists(baseTargetPath)) FileUtils.deleteDirectory(baseTargetPath.toFile)
      allowedSubDirs.foreach(dir => minifyDir(dir))
    }

    val t1 = System.currentTimeMillis()
    println("--- Minifier " + dirPrefix + " finished in " + (t1 - t0) + " ms ---")
  }

  // ------------------------------- Private & protected methods -------------------------------

  protected def minifyFile(path: Path, sourceDir: Path, targetDir: Path, fileExtTransform: FileExtTransform): Unit = {
    println("Minify: " + path)
    val targetPath: Path = Paths.get(fileExtTransform.transform(targetDir.resolve(sourceDir.relativize(path)).toString))
    minifier(IOUtils.readString(path)) match {
      case Left(errors) =>
        System.err.println(errors)
        sys.error("Error minifying " + path)
      case Right(code) =>
        saveMinified(path, targetPath, code)
    }
  }

  private def saveMinified(sourcePath: Path, targetPath: Path, code: String): Unit = {
    Files.createDirectories(targetPath.getParent)
    IOUtils.writeToFile(targetPath, code)
    Files.setLastModifiedTime(targetPath, Files.getLastModifiedTime(sourcePath))
  }
}


class ScriptMinifierForStage(dirPrefix: String,
                             sourceFileEnding: String,
                             minifier: String => Either[String, String],
                             subDir: String)
  extends ScriptMinifier(dirPrefix, sourceFileEnding, minifier, Seq(subDir)) {

  override protected def baseTargetDir: String = "target/assets-release"

  /**
    * Minify specified files only.
    * Used on `stage` build step.
    */
  def minifyFiles(subPaths: Seq[String], targetFileExt: String): Unit = {
    println("--- Minifying files: " + subPaths.mkString(", ") + " ---")
    val executorThreadNum = 4
    val t0 = System.currentTimeMillis()
    Executors.withSynchronousQueueExecutor("minifier-%d", executorThreadNum) {executor =>
      if (Files.exists(baseTargetPath)) FileUtils.deleteDirectory(baseTargetPath.toFile)
      val sourceDir: Path = Resources.localPathDev(subDir)
      val targetDir: Path = baseTargetPath
      val fileExtTransform = new FileExtTransform(targetFileExt)
      for (subPath <- subPaths) {
        val path = sourceDir.resolve(subPath)
        require(Files.isRegularFile(path), "File not found: " + path)
        executor.execute {
          minifyFile(path, sourceDir, targetDir, fileExtTransform)
        }
      }
    }

    val t1 = System.currentTimeMillis()
    println("--- Minifier finished in " + (t1 - t0) + " ms ---")
  }
}
