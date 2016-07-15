package webby.mvc.script

import java.nio.file.{Files, Path}
import java.util

import com.google.common.base.Charsets
import com.google.javascript.jscomp._
import webby.commons.io.IOUtils

import scala.collection.JavaConversions._

/**
  * Класс, работающий напрямую с google closure compiler.
  * Запускает компиляцию closure модулей.
  *
  * Requires sbt dependency
  * {{{
  *   deps += "com.google.javascript" % "closure-compiler" % "v20160619"
  * }}}
  */
class GoogleClosureCompiler(externFiles: Seq[Path],
                            jQueryPrependPath: Path,
                            resultDir: Path,
                            commonIncludes: Seq[String],
                            restModuleWrapper: String => String) {

  lazy val prependScript: String = IOUtils.readString(jQueryPrependPath)

  def compileModule(moduleName: String, jsFiles: Iterable[String], jsRestFiles: Iterable[String]): Seq[Path] = {
    val compiler: Compiler = new Compiler(System.err)
    val options = new CompilerOptions

    CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(options)

    options.setOutputCharset(Charsets.UTF_8)
    options.setTrustedStrings(true)
    options.generateExports = true
    options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT6_STRICT)
    options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5)

    val externs: util.List[SourceFile] = AbstractCommandLineRunner.getBuiltinExterns(options.getEnvironment)
    externFiles.foreach(p => externs.add(SourceFile.fromFile(p.toFile)))

    def mkSource(fileName: String): SourceFile = SourceFile.fromFile(fileName, Charsets.UTF_8)

    val jsModules = new util.ArrayList[JSModule]()
    val entryPoints = new util.ArrayList[ModuleIdentifier]()

    val mainModule = new JSModule(moduleName)
    jsModules.add(mainModule)
    entryPoints.add(ModuleIdentifier.forClosure(moduleName + "_js"))
    commonIncludes.foreach(p => mainModule.add(mkSource(p)))
    jsFiles.foreach(p => mainModule.add(mkSource(p)))

    if (jsRestFiles.nonEmpty) {
      val jsFileSet = jsFiles.toSet
      val restModule = new JSModule(moduleName + "_rest")
      jsModules.add(restModule)
      entryPoints.add(ModuleIdentifier.forClosure(moduleName + "_rest_js"))
      restModule.addDependency(mainModule)
      jsRestFiles.foreach(p => if (!jsFileSet.contains(p)) restModule.add(mkSource(p)))
    }

    options.setDependencyOptions(new DependencyOptions()
      .setDependencyPruning(true)
      .setDependencySorting(true)
      .setMoocherDropping(true)
      .setEntryPoints(entryPoints))

    compiler.initOptions(options)
    val result: Result = compiler.compileModules(externs, jsModules, options)

    if (result.errors.nonEmpty) Nil
    else {
      jsModules.map {jsMod =>
        val source: String = compiler.toSource(jsMod)
        val totalSource: String =
          if (jsMod eq mainModule) {
            prependScript + source
          } else {
            // rest module
            restModuleWrapper(source)
            "if(!window._rest){_restm=[];_rest=function(m){_restm.push(m)}};_rest(function(){" + source + "})"
          }
        val resultPath: Path = resultDir.resolve(jsMod.getName + ".js")
        Files.createDirectories(resultPath.getParent)
        IOUtils.writeToFile(resultPath, totalSource)
        resultPath
      }
    }
  }
}
