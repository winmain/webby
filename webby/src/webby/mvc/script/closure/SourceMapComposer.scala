package webby.mvc.script.closure

import java.lang.StringBuilder
import java.nio.file.{Files, Path, Paths}

import com.google.debugging.sourcemap.{FilePosition, SourceMapGeneratorV3}
import com.google.javascript.jscomp.{SourceFile, SourceMapInput}
import org.apache.commons.lang3.StringUtils
import webby.commons.io.Using
import webby.commons.io.codec.Base64

/**
  * Wrapper for [[SourceMapGeneratorV3]] to make source maps.
  * Also, this class able to transform source maps from input code to resulting big source map.
  */
class SourceMapComposer(basePath: Path) {
  private val normBasePath: Path = basePath.toAbsolutePath.normalize()
  val gen: SourceMapGeneratorV3 = new SourceMapGeneratorV3

  /**
    * Adds source file to source map and prepare it to write to composed file
    * @param source     Source file to add
    * @param lineNumber Current line number in output composed file
    * @return (source, lines) where:
    *         source - prepared source file (without `//# sourceMappingURL=`)
    *         lines - lines in the prepared source file
    */
  def addSourceFile(source: SourceFile, lineNumber: Int): (String, Int) = {
    val code: String = source.getCode

    SourceMapComposer.SourceMappingUrlR.findFirstMatchIn(code) match {
      case Some(m) =>
        val innerSourceMapFilePath = Paths.get(source.getName).resolveSibling(m.group(1))
        val innerSourceMapInput = new SourceMapInput(SourceFile.fromFile(innerSourceMapFilePath.toFile))
        //gen.setSourceRoot(innerSourceMapInput.getSourceMap.getSourceRoot)
        gen.setSourceRoot(normBasePath.toString)
        innerSourceMapInput.getSourceMap.visitMappings {
          (sourceName: String, symbolName: String, sourceStartPos: FilePosition, startPos: FilePosition, endPos: FilePosition) =>
            val path: Path = cutBasePath(Paths.get(sourceName))
            gen.addMapping(path.toString, path.toString, sourceStartPos,
              new FilePosition(startPos.getLine + lineNumber, startPos.getColumn),
              new FilePosition(endPos.getLine + lineNumber, endPos.getColumn))
        }
        val cleanCode = code.substring(0, m.start)
        (cleanCode, StringUtils.countMatches(cleanCode, '\n'))

      case None =>
        val linesInCode = StringUtils.countMatches(code, '\n')
        gen.addMapping(source.getName, source.getName,
          new FilePosition(0, 0),
          new FilePosition(lineNumber, 0),
          new FilePosition(lineNumber + linesInCode + 1, 0))
        (code, linesInCode)
    }
  }

  /**
    * Write source map to specified file.
    */
  def writeToFile(path: Path, name: String): Unit = {
    Using(Files.newBufferedWriter(path)) {out =>
      gen.appendTo(out, name)
    }
  }

  /**
    * Makes `//# sourceMappingURL=` string
    */
  def makeSourceMappingUrl(url: String): String = "//# sourceMappingURL=" + url

  /**
    * Makes `//# sourceMappingURL=` with encoded data
    */
  def makeSourceMappingUrlInplace: String = {
    val sb = new StringBuilder(1024)
    gen.appendTo(sb, "")
    val encoded = Base64.encodeToString(sb.toString.getBytes, false)
    "//# sourceMappingURL=data:application/json;base64," + encoded
  }


  private def cutBasePath(path: Path): Path = {
    val normPath = path.toAbsolutePath.normalize()
    if (normPath.startsWith(normBasePath)) {
      normPath.subpath(normBasePath.getNameCount, normPath.getNameCount)
    } else path
  }
}

object SourceMapComposer {
  val SourceMappingUrlR = "//# sourceMappingURL=([^\n]+)$".r
}
