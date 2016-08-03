package webby.mvc.script

import java.nio.file.{Files, Path}
import java.util.function.Consumer
import javax.annotation.Nullable

import com.google.common.base.Charsets
import org.apache.commons.io.FileUtils
import webby.api.App
import webby.commons.concurrent.Executors
import webby.commons.io.{IOUtils, Resources}

class ScriptMinifier(dirPrefix: String,
                     fileEnding: String,
                     minifier: String => Either[String, String],
                     allowedSubDirs: Seq[String]) {
  val log = webby.api.Logger(getClass)

  protected def baseTargetDir: String = "target/asset-resources"

  protected val baseSourcePath: Path = Resources.localPathDev("app")
  protected val baseTargetPath: Path = Resources.localPathDev(baseTargetDir + "/" + dirPrefix)

  protected def makeSourcePath(pathFromApp: String, subDir: String): Path = baseSourcePath.resolve(pathFromApp)
  protected def makeTargetPath(pathFromApp: String, subDir: String): Path = baseTargetPath.resolve(pathFromApp)

  def getAllowedSubDir(pathFromApp: String): Option[String] =
    allowedSubDirs.find(subDir => pathFromApp.startsWith(subDir + "/"))

  protected def loadDev(pathFromApp: String, subDir: String): Either[String, String] = {
    val sourcePath = makeSourcePath(pathFromApp, subDir)
    val targetPath = makeTargetPath(pathFromApp, subDir)
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
        val sourceDir = makeSourcePath(subDir, subDir)
        val targetDir = makeTargetPath(subDir, subDir)
        Files.walk(sourceDir).forEach(new Consumer[Path] {
          override def accept(path: Path): Unit = {
            if (Files.isRegularFile(path) && path.getFileName.toString.endsWith(fileEnding)) {
              executor.execute {
                println("Minify: " + path)
                val targetPath: Path = targetDir.resolve(sourceDir.relativize(path))
                minifier(IOUtils.readString(path)) match {
                  case Left(errors) =>
                    System.err.println(errors)
                    sys.error("Error minifying " + path)
                  case Right(code) =>
                    saveMinified(path, targetPath, code)
                }
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

  private def saveMinified(sourcePath: Path, targetPath: Path, code: String): Unit = {
    Files.createDirectories(targetPath.getParent)
    IOUtils.writeToFile(targetPath, code)
    Files.setLastModifiedTime(targetPath, Files.getLastModifiedTime(sourcePath))
  }
}
