package webby.mvc.script
import com.google.javascript.jscomp.SourceFile
import webby.commons.io.Resources
import webby.mvc.script.compiler.ExternalHaxeCompiler
import webby.mvc.{AppStub, StdPaths}

object GoogleClosure {
  /**
    * Вернуть builder для создания экземпляра севрера-компилятора [[GoogleClosureServer]].
    *
    * Пример использования:
    * {{{
    *   GoogleClosure.serverBuilder
    *     .commonSourceDirsWithProfile(profile)
    *     .useCoffeeScript
    *     .useJade
    *     .useJQuery(Public.jQueryPath, Public.jQueryMinPath)
    *     .extern(SourceFile.fromFile(Paths.assets.resolve("js/lib/externs.js").toFile))
    * }}}
    * @return
    */
  def serverBuilder = new GoogleClosureServerBuilder

  def sourceFromUrl(path: String): SourceFile = SourceFile.builder().withOriginalPath(path).buildFromUrl(Resources.url(path))

  def jQueryExtern: SourceFile = sourceFromUrl("js/google-closure-externs/jquery-1.9.js")

  /**
    * Запустить компиляцию js-модулей используя advanced режим gcc.
    * Этот метод вызывается при сборке ресурсов для production.
    *
    * Пример использования:
    * {{{
    *   object GoogleClosureAdvancedCompiler {
    *     def main(args: Array[String]) {
    *       require(args.length == 1, "Must be one arg: profile name")
    *       GoogleClosure.runAdvancedCompiler(
    *         AppPaths,
    *         GoogleClosureServers.builder(args(0)),
    *         Seq("main", "mobile", "adm"))
    *     }
    *   }
    * }}}
    */
  def runAdvancedCompiler(appPaths: => StdPaths.Value,
                          builder: => GoogleClosureServerBuilder,
                          compileModules: Seq[String]): Unit = {
    println("--- Google Closure Compiler step ---")
    val t0 = System.currentTimeMillis()
    AppStub.withAppNoPluginsDev {
      val paths = appPaths
      val server = builder
        .targetGccDir(paths.root.resolve("target/assets-release/js"))
        .build
      server.compileClosure(compileModules)
    }
    val t1 = System.currentTimeMillis()
    println("--- Google Closure Compiler finished in " + (t1 - t0) + " ms ---")
  }


  def serverBuilderForHaxeTests = {
    GoogleClosure.serverBuilder
      .jsSourceDirs(StdPaths.getHaxeValue.haxeCp)
      .preCompiler(new ExternalHaxeCompiler(profile = "test", StdPaths.getHaxeValue))
      .targetDir(StdPaths.get.jsTestAssetType.targetPath)
      .targetGccDir(StdPaths.get.jsTestAssetType.targetPath)
      .muteGCCWarnings(true)
  }
}
