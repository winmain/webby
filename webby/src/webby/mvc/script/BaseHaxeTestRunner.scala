package webby.mvc.script
import java.nio.file.Path

import webby.commons.io.IOUtils
import webby.mvc.{AppStub, StdPaths}

class BaseHaxeTestRunner(haxeTestClassName: String) {
  def gccServerBuilderForTests: GoogleClosureServerBuilder = GoogleClosure.serverBuilderForHaxeTests

  def nodeJsCommand: String = "nodejs"

  def getComposedOutFile: Path = gccServerBuilderForTests._targetDir.resolve(haxeTestClassName + "-composed.js")

  def appStubWrapper(block: => Any): Unit = AppStub.withAppNoPluginsTest(block)

  def haxeCp: Seq[Path] = StdPaths.getHaxeValue.haxeCp

  def main(args: Array[String]): Unit = {
    appStubWrapper {
      val composedOutFile = getComposedOutFile
      val gccServer = gccServerBuilderForTests.build

      gccServer.composeDev(haxeTestClassName + ".js") match {
        case Some(compileResult) =>
          // Some hacks for nodejs
          var jsResult = compileResult.result
          jsResult = "goog = {};\nthis['goog'] = goog;\n" + jsResult

          IOUtils.writeToFile(composedOutFile, jsResult)
          sys.exit(runNodeJs(composedOutFile, args.toVector))
        case None =>
          println("Cannot find test entrypoint: " + haxeTestClassName + ".hx")
          println("Using source paths:")
          println(haxeCp.mkString("\n"))
          sys.exit(-1)
      }
    }
  }

  // run nodejs
  protected def runNodeJs(jsFile: Path, args: Iterable[String] = Nil): Int = {
    val proc: Process = Runtime.getRuntime.exec(Array(nodeJsCommand, jsFile.toString) ++ args)
    val result: String = IOUtils.readString(proc.getInputStream)
    val errors: String = IOUtils.readString(proc.getErrorStream)
    System.out.println(result)
    System.err.println(errors)
    proc.waitFor()
    proc.exitValue()
  }
}


object HaxeTestRunner extends BaseHaxeTestRunner("HaxeTests")
