package webby.mvc.script

import java.nio.file.{Files, Path}
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
  */
class GoogleClosureCompiler(externs: Seq[SourceFile],
                            prepends: Seq[SourceFile],
                            resultDir: Path,
                            commonIncludes: Seq[SourceFile],
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

    if (result.errors.nonEmpty) Nil
    else {
      jsModules.asScala.map {jsMod =>
        val source: String = compiler.toSource(jsMod)
        val totalSource: String =
          if (jsMod eq mainModule) {
            new SB {
              prepends.foreach(+_.getCode)
              +source
            }.str
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

class MuteAllWarningsGuard extends WarningsGuard {
  override def level(error: JSError): CheckLevel = {
    error.getDefaultLevel match {
      case CheckLevel.ERROR => CheckLevel.ERROR
      case _ => CheckLevel.OFF
    }
  }
}
