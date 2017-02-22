package webby.mvc.script

import java.nio.charset.StandardCharsets
import java.util

import com.google.javascript.jscomp._

/**
  * Запускатор Google Closure Compiler в качестве простого минификатора скриптов.
  * Advanced optimizations здесь не включаются, поэтому к конвертируемому коду нет строгих требований.
  *
  * Requires sbt dependency
  * {{{
  *   deps += "com.google.javascript" % "closure-compiler" % "v20170124"
  * }}}
  */
object GoogleClosureSimpleCompiler {
  def minify(code: String, langIn: CompilerOptions.LanguageMode, langOut: CompilerOptions.LanguageMode): Either[Array[JSError], String] = {
    val compiler: Compiler = new Compiler(System.err)
    val options = new CompilerOptions

    CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options)

    options.setOutputCharset(StandardCharsets.UTF_8)
    options.setTrustedStrings(true)
    options.setLanguageIn(langIn)
    options.setLanguageOut(langOut)

    compiler.initOptions(options)

    val externs = AbstractCommandLineRunner.getBuiltinExterns(options.getEnvironment)
    val result: Result = compiler.compile(externs, util.Arrays.asList(SourceFile.fromCode("code", code)), options)
    if (result.success) Right(compiler.toSource)
    else Left(result.errors)
  }

  def minifyEs5(code: String): Either[Array[JSError], String] =
    minify(code, CompilerOptions.LanguageMode.ECMASCRIPT5, CompilerOptions.LanguageMode.ECMASCRIPT5)
}
