package webby.mvc.script
import java.nio.file.Path

import com.google.javascript.jscomp.SourceFile

/**
  * Класс, описывающий два js-файла - исходник и его минифицированная версия для продакшна.
  */
case class JsSourcePair(source: SourceFile, minified: SourceFile)

object JsSourcePair {
  def apply(source: Path, minified: Path): JsSourcePair =
    JsSourcePair(SourceFile.fromFile(source.toFile), SourceFile.fromFile(minified.toFile))
}
