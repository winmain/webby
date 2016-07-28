package webby.mvc.script
import com.google.javascript.jscomp.SourceFile
import webby.commons.io.Resources

import scala.util.Try

trait GoogleClosureLibSource {
  def forPath(path: String): SourceFile
  def baseJs: SourceFile
  def depsJs: SourceFile
}

class ResourceGoogleClosureLibSource(prefixPath: String = "goog/") extends GoogleClosureLibSource {
  override def forPath(subPath: String): SourceFile = {
    val path = prefixPath + subPath
    SourceFile.builder().withOriginalPath(path).buildFromUrl(Resources.url(path))
  }

  private def forPathAndCheck(subPath: String): SourceFile = {
    val sourceFile = forPath(subPath)
    Try(sourceFile.getCode).getOrElse(sys.error("File " + prefixPath + subPath + " not found in resources. Missing dependency?"))
    sourceFile
  }

  override val baseJs: SourceFile = forPathAndCheck("base.js")
  override val depsJs: SourceFile = forPathAndCheck("deps.js")
}

object DefaultResourceGoogleClosureLibSource extends ResourceGoogleClosureLibSource()
