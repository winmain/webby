package webby.mvc.script
import com.google.javascript.jscomp.SourceFile

trait GoogleClosureLibSource {
  def forPath(path: String): SourceFile
  def baseJs: SourceFile
  def depsJs: SourceFile
}

class ResourceGoogleClosureLibSource(prefixPath: String = "goog/") extends GoogleClosureLibSource {
  override def forPath(subPath: String): SourceFile = {
    val path = prefixPath + subPath
    SourceFile.builder().withOriginalPath(path).buildFromUrl(getClass.getClassLoader.getResource(path))
  }

  override val baseJs: SourceFile = forPath("base.js")
  override val depsJs: SourceFile = forPath("deps.js")
}

object DefaultResourceGoogleClosureLibSource extends ResourceGoogleClosureLibSource()
