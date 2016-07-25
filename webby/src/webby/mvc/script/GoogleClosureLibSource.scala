package webby.mvc.script
import com.google.javascript.jscomp.SourceFile
import webby.commons.io.Resources

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

  override val baseJs: SourceFile = forPath("base.js")
  override val depsJs: SourceFile = forPath("deps.js")
}

object DefaultResourceGoogleClosureLibSource extends ResourceGoogleClosureLibSource()
