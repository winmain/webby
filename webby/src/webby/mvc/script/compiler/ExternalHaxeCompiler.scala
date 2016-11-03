package webby.mvc.script.compiler

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import com.fasterxml.jackson.core.JsonParser
import org.apache.commons.lang3.StringUtils
import webby.commons.io.StdJs
import webby.commons.text.SB

import scala.collection.mutable.ArrayBuffer

/**
  * Haxe-js компилятор, предназначенный для работы внутри [[webby.mvc.script.GoogleClosureServer]].
  *
  * @param sourceDirs      Каталоги с haxe исходниками
  * @param profile         Профиль сборки (dev, jenkins, prod)
  * @param compilerOptions Опции компилятора
  */
case class ExternalHaxeCompiler(sourceDirs: Seq[Path],
                                profile: String,
                                compilerOptions: HaxeCompilerOptions) extends ScriptCompiler {
  def sourceFileExt: String = "hx"
  def targetFileExt: String = "js"
  def targetContentType: String = "text/javascript"

  private val errorBeautifier = new HaxeErrorBeautifier

  override def compile(source: String, sourcePath: Path): Either[String, String] = sys.error("Unsupported method")

  override def compileFile(sourcePath: Path, outputPath: Path): Either[String, Unit] = {
    val mainClass = StringUtils.removeEnd(sourcePath.getFileName.toString, ".hx")
    val command = ArrayBuffer(compilerOptions.runBinary, "-main", mainClass, "-js", outputPath.toString)
    sourceDirs.foreach {include =>
      command += "-cp"
      command += include.toString
    }
    command += "-dce"
    command += compilerOptions.deadCodeElimination
    command ++= compilerOptions.moreOptions

    command += "-D"
    command += "profile=" + profile

    val env = Seq("HAXE_STD_PATH=" + compilerOptions.haxeStdPath.toString)

    runCommonProcess(command, env = env) match {
      case Left(errors) => Left(errorBeautifier.beautify(errors))
      case Right(_) =>
        var body = new String(Files.readAllBytes(outputPath), StandardCharsets.UTF_8)
        try {
          body = postProcess(body)
        } catch {
          case e: Exception =>
            // В случае ошибки в postProcess, удаляем файл, чтобы он не считался как законченный собранный файл
            Files.delete(outputPath); throw e
        }
        Files.write(outputPath, body.getBytes(StandardCharsets.UTF_8))
        Right(Unit)
    }
  }

  // Этот флаг мы включаем для того, чтобы GoogleClosureServer не компилировал сразу все *.hx
  // файлы во всех каталогах. Вместо этого, он будет компилировать только те файлы, которые нужны
  // по запросу юзера.
  override def lazyCompiler: Boolean = true

  private def postProcess(body: String): String = {
    import scala.collection.JavaConversions._
    val mapper = StdJs.get.newMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)

    val globalPrepend = new SB()
    val googs = new SB()

    def addGoogRequire(v: String): Unit = googs + "goog.require(\"" + v + "\");\n"
    def addGoogProvide(v: String): Unit = googs + "goog.provide(\"" + v + "\");\n"

    // Обработать метаинформацию, записанную в @аннотациях haxe классов.
    var b: String = MetaR.replaceAllIn(body, {m =>
      // Обработаем и уберём все __meta__ параметры. Это аннотации к классам и методам, записанные так:
      // @globalPrepend, @googRequire
      val tree = mapper.readTree(m.group(1))
      for (entry <- tree.get("obj").fields()) {
        entry.getKey match {
          case "globalPrepend" =>
            // Аннотация просто вставляет код как есть в самое начало файла, до основной closure функции
            // Пример: @globalPrepend('// scriptEntryPoint')
            entry.getValue.foreach {node => globalPrepend + node.textValue() + '\n'}

          case "googRequire" =>
            // Вставляет goog.require() директивы
            // Пример: @googRequire('goog.net.cookies', 'goog.events')
            entry.getValue.foreach {node => addGoogRequire(node.textValue())}

          case "googProvide" =>
            // Вставляет goog.provide() директивы
            // Пример: @googProvide('MainEntryPoint_js')
            entry.getValue.foreach {node => addGoogProvide(node.textValue())}

          case unknown =>
            sys.error("Unknown annotation " + unknown + " in haxe file. Supported annotations: " +
              "@globalPrepend, @googRequire, @googProvide.")
        }
      }
      ""
    })

    // Обработать аннотации @:jsRequire. Вызовы функций require будут заменены на директивы goog.require.
    b = JsRequireR.replaceAllIn(b, {m =>
      val varDecl = m.group(1)
      val require = m.group(2)
      val maybeClassName = m.group(3)
      val className: String = if (maybeClassName == null) require else maybeClassName

      addGoogRequire(require)
      varDecl + className + ";\n"
    })

    globalPrepend.str + googs.str + b
  }

  private val MetaR = "(?m)^[a-zA-Z0-9_]+\\.__meta__ *= *(\\{[^\n]+\\});\n?".r
  private val JsRequireR = "(?m)(^var [a-zA-Z0-9_]+ *= *)require\\([\"']([^\"']+)[\"']\\)(?:\\.([^;]+))?;\n?".r
}

class HaxeCompilerOptions(haxeHomeDir: Path) {
  /** Путь к бинарнику haxe */
  var runBinary: String = haxeHomeDir.resolve("haxe").toString

  /** Путь к стандартной библиотеке haxe. Записывается в env HAXE_LIBRARY_PATH. */
  var haxeStdPath: Path = haxeHomeDir.resolve("std")

  /** Параметр "-dce" */
  var deadCodeElimination: String = "full"

  /** Флаг "-debug". Если он присутствует, haxe генерирует source map. */
  var debug: Boolean = false

  /** Дополнительные опции компиляции */
  var moreOptions: Seq[String] = Nil
}
