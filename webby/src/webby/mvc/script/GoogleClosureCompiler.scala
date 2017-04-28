package webby.mvc.script

import java.nio.file.{Files, Path, Paths}
import java.util

import com.google.common.base.Charsets
import com.google.javascript.jscomp._
import webby.commons.io.IOUtils
import webby.commons.text.SB

import scala.collection.JavaConverters._

/**
  * Класс, работающий напрямую с google closure compiler.
  * Запускает компиляцию closure модулей.
  *
  * Requires sbt dependency
  * {{{
  *   deps += "com.google.javascript" % "closure-compiler" % "v20170124"
  * }}}
  *
  * @param externs            Javascript externs
  * @param mainModulePrepends This scripts will be simply prepended as is in main module
  * @param resultDir          The output path for compiled js files
  * @param commonIncludes     Google closure compiler library files and other dependencies
  * @param sourceMapConfig    Configuration for source map generation
  * @param restModuleWrapper  Wrap rest module in this javascript code
  * @param muteAllWarnings    Add [[MuteAllWarningsGuard]] to mute Google closure compiler warnings
  */
class GoogleClosureCompiler(externs: Seq[SourceFile],
                            mainModulePrepends: Seq[SourceFile],
                            resultDir: Path,
                            commonIncludes: Seq[SourceFile],
                            sourceMapConfig: Option[GccSourceMapConfig] = None,
                            restModuleWrapper: String => String,
                            muteAllWarnings: Boolean = false) {

  def compileModule(moduleName: String, jsFiles: Iterable[SourceFile], jsRestFiles: Iterable[SourceFile]): Seq[Path] = {
    val compiler: Compiler = new Compiler(System.err)
    val options = new CompilerOptions

    CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(options)

    options.setOutputCharset(Charsets.UTF_8)
    options.setTrustedStrings(true)
    options.generateExports = true
    options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT6_TYPED)
    options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5)

    // Enable sourcemaps
    sourceMapConfig.foreach {cfg =>
      options.setSourceMapIncludeSourcesContent(true)
      options.setSourceMapOutputPath(cfg.resultDir.toString)
    }

    // Disable warnings in Google closure library
    options.addWarningsGuard(ByPathWarningsGuard.forPath(util.Arrays.asList("goog"), CheckLevel.OFF))

    if (muteAllWarnings) {
      options.addWarningsGuard(new MuteAllWarningsGuard)
    }

    val externList: util.List[SourceFile] = AbstractCommandLineRunner.getBuiltinExterns(options.getEnvironment)
    externList.addAll(externs.asJavaCollection)

    val jsModules = new util.ArrayList[JSModule]()
    val entryPoints = new util.ArrayList[ModuleIdentifier]()

    val mainModule = new JSModule(moduleName)
    jsModules.add(mainModule)
    entryPoints.add(ModuleIdentifier.forClosure(moduleName + "_js"))
    commonIncludes.foreach(p => mainModule.add(p))
    jsFiles.foreach(p => mainModule.add(p))

    if (jsRestFiles.nonEmpty) {
      val jsFileSet = jsFiles.toSet
      val restModule = new JSModule(moduleName + "_rest")
      jsModules.add(restModule)
      entryPoints.add(ModuleIdentifier.forClosure(moduleName + "_rest_js"))
      restModule.addDependency(mainModule)
      jsRestFiles.foreach(p => if (!jsFileSet.contains(p)) restModule.add(p))
    }

    options.setDependencyOptions(new DependencyOptions()
      .setDependencyPruning(true)
      .setDependencySorting(true)
      .setMoocherDropping(true)
      .setEntryPoints(entryPoints))

    compiler.initOptions(options)
    val result: Result = compiler.compileModules(externList, jsModules, options)

    // Add sources to sourcemap
    commonIncludes.foreach(p => compiler.getSourceMap.addSourceFile(p))
    jsFiles.foreach(p => compiler.getSourceMap.addSourceFile(p))

    if (result.errors.nonEmpty) Nil
    else {
      jsModules.asScala.map {jsMod =>
        val source: String = compiler.toSource(jsMod)
        val sourceMapFooter: String = sourceMapConfig match {
          case None => ""
          case Some(cfg) => "\n//# sourceMappingURL=" + cfg.basePath + jsMod.getName + cfg.suffix
        }
        val totalSource: String =
          if (jsMod eq mainModule) {
            new SB {
              mainModulePrepends.foreach(+_.getCode)
              +source + sourceMapFooter
            }.str
          } else {
            // rest module
            restModuleWrapper(source + sourceMapFooter)
          }
        val resultPath: Path = resultDir.resolve(jsMod.getName + ".js")
        Files.createDirectories(resultPath.getParent)
        IOUtils.writeToFile(resultPath, totalSource)

        // Save sourcemap
        sourceMapConfig.foreach {cfg =>
          Files.createDirectories(cfg.resultDir)
          val writer = Files.newBufferedWriter(cfg.resultDir.resolve(jsMod.getName + cfg.suffix))
          compiler.getSourceMap.appendTo(writer, jsMod.getName)
          writer.close()
        }

        resultPath
      }
    }
  }
}

class MuteAllWarningsGuard extends WarningsGuard {
  override def level(error: JSError): CheckLevel = {
    error.getDefaultLevel match {
      case CheckLevel.ERROR => CheckLevel.ERROR
      case _ => CheckLevel.OFF
    }
  }
}

/**
  * @param resultDir            The output path for the source map
  * @param basePath             The base path prefix to source map in output files including trailing slash '/'
  * @param suffix               Source map file suffix
  * @param includeSourceContent Whether to include full file contents in the source map.
  */
case class GccSourceMapConfig(resultDir: Path = Paths.get("target/asset-resources/js-sourcemap"),
                              basePath: String = "/js-sourcemap/",
                              suffix: String = ".js.map",
                              includeSourceContent: Boolean = true)
