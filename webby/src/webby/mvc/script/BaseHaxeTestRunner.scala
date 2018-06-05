package webby.mvc.script
import java.nio.file.{Files, Path, Paths}

import webby.commons.io.{IOUtils, Using}
import webby.mvc.{AppStub, StdPaths}

class BaseHaxeTestRunner(haxeTestClassName: String) {
  def gccServerBuilderForTests: GoogleClosureServerBuilder = GoogleClosure.serverBuilderForHaxeTests

  def nodeJsCommand: String = "node"

  def getComposedOutFile: Path = gccServerBuilderForTests._targetDir.resolve(haxeTestClassName + "-composed.js")

  def getComposedOutSourceMapFile: Path = Paths.get(getComposedOutFile.toString + ".map")
  def getComposedOutSourceMapFileName: String = getComposedOutSourceMapFile.getFileName.toString

  def appStubWrapper(block: => Any): Unit = AppStub.withAppNoPluginsTest(block)

  def haxeCp: Seq[Path] = StdPaths.getHaxeValue.haxeCp

  def main(args: Array[String]): Unit = {
    appStubWrapper {
      val composedOutFile: Path = getComposedOutFile
      val gccServer = gccServerBuilderForTests.build

      // Some hacks for nodejs
      val prependString = "goog = {};\nthis['goog'] = goog;\n"

      gccServer.composeDev(haxeTestClassName + ".js", prependString = prependString) match {
        case Some(compileResult) =>
          var jsResult = compileResult.result
          Using(Files.newBufferedWriter(composedOutFile)) {writer =>
            writer.write(jsResult)
            writer.write("\n" + compileResult.sourceMapComposer.makeSourceMappingUrl(getComposedOutSourceMapFileName))
          }
          compileResult.sourceMapComposer.writeToFile(getComposedOutSourceMapFile, composedOutFile.getFileName.toString)
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


/**
  * @see ExampleHaxeTests.hx file
  */
object HaxeTestRunner extends BaseHaxeTestRunner("HaxeTests")
