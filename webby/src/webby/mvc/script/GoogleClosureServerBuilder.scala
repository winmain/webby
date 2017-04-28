package webby.mvc.script
import java.nio.file.Path

import com.google.javascript.jscomp.SourceFile
import webby.api.App
import webby.mvc.StdPaths
import webby.mvc.script.compiler.{ExternalCoffeeScriptCompiler, ExternalJadeClosureCompiler, ScriptCompiler}

/**
  * Билдер для [[GoogleClosureServer]].
  *
  * Рекомендуется создавать его методом [[GoogleClosure.serverBuilder]]
  */
class GoogleClosureServerBuilder {
  var _libSource: GoogleClosureLibSource = _
  def libSource(v: GoogleClosureLibSource) = {_libSource = v; this}

  var _jsSourceDirs: Seq[Path] = Nil
  def jsSourceDirs(v: Seq[Path]) = {_jsSourceDirs = v; this}
  def jsSourceDir(add: Path) = {_jsSourceDirs +:= add; this}
  def addJsSourceDirs(v: Seq[Path]) = {_jsSourceDirs ++= v; this}
  def simpleSourceDir = jsSourceDirs(Seq(StdPaths.get.jsAssetType.assetsPath))
  def commonSourceDirsWithProfile(profile: String) = jsSourceDirs(Vector(StdPaths.get.jsAssetType.assetsPath, StdPaths.get.assetsProfile(profile)))

  var _preCompilers: List[ScriptCompiler] = Nil
  def preCompilers(v: List[ScriptCompiler]) = {_preCompilers = v; this}
  def preCompiler(v: ScriptCompiler) = {_preCompilers ::= v; this}

  var _prepends: Seq[JsSourcePair] = Nil
  def prepends(v: Seq[JsSourcePair]) = {_prepends = v; this}
  def prepend(v: JsSourcePair) = {_prepends +:= v; this}

  var _externs: Seq[SourceFile] = Nil
  def externs(v: Seq[SourceFile]) = {_externs = v; this}
  def extern(v: SourceFile) = {_externs +:= v; this}

  var _targetDir: Path = _
  def targetDir(v: Path) = {_targetDir = v; this}

  var _targetGccDir: Path = _
  def targetGccDir(v: Path) = {_targetGccDir = v; this}

  var _remapEntryPoints: Map[String, String] = Map.empty
  def remapEntryPoints(v: Map[String, String]) = {_remapEntryPoints = v; this}

  var _errorRenderer: ScriptErrorRenderer = _
  def errorRenderer(v: ScriptErrorRenderer) = {_errorRenderer = v; this}
  def jsErrorRenderer = errorRenderer(new JsScriptErrorRenderer)

  var _muteGCCWarnings: Boolean = false
  def muteGCCWarnings(v: Boolean) = {_muteGCCWarnings = v; this}

  var _sourceMapConfig: GccSourceMapConfig = null
  def sourceMapConfig(v: GccSourceMapConfig) = {_sourceMapConfig = v; this}

  // ------------------------------- Common uses -------------------------------

  def useCoffeeScript = preCompiler(ExternalCoffeeScriptCompiler(goog = true))
  def useJade = preCompiler(ExternalJadeClosureCompiler(noDebug = !App.isDev, pretty = App.isDev))

  def useJQuery(jsSourcePair: JsSourcePair): GoogleClosureServerBuilder =
    prepend(jsSourcePair).extern(GoogleClosure.jQueryExtern)
  def useJQuery(sourcePublicPath: String, minPublicPath: String): GoogleClosureServerBuilder =
    useJQuery(JsSourcePair(StdPaths.get.public.resolve(sourcePublicPath), StdPaths.get.public.resolve(minPublicPath)))

  // ------------------------------- Build & inner methods -------------------------------

  private def withDefault[T <: AnyRef](v: T, onNull: => T): T = if (v == null) onNull else v

  def build = {
    require(_jsSourceDirs.nonEmpty, "jsSourceDirs cannot be empty")

    new GoogleClosureServer(
      libSource = withDefault(_libSource, DefaultResourceGoogleClosureLibSource),
      jsSourceDirs = _jsSourceDirs,
      preCompilers = _preCompilers,
      prepends = _prepends,
      externs = _externs,
      targetDir = withDefault(_targetDir, StdPaths.get.jsAssetType.targetAssetsPath),
      targetGccDir = withDefault(_targetGccDir, StdPaths.get.jsGccAssetType.targetAssetsPath),
      remapEntryPoints = _remapEntryPoints,
      sourceMapConfig = Option(_sourceMapConfig),
      errorRenderer = withDefault(_errorRenderer, new DefaultScriptErrorRenderer),
      muteGCCWarnings = _muteGCCWarnings
    )
  }
}
