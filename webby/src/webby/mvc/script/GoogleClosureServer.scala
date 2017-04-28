package webby.mvc.script

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.google.common.base.CharMatcher
import com.google.common.net.HttpHeaders
import com.google.javascript.jscomp.SourceFile
import io.netty.handler.codec.http.HttpResponseStatus
import org.apache.commons.lang3.StringUtils
import webby.api.mvc.{PlainResult, RequestHeader, ResultException, Results}
import webby.commons.io.Using
import webby.commons.text.SB
import webby.commons.text.StringWrapper.wrapper
import webby.commons.time.StdDates
import webby.mvc.script.compiler.ScriptCompiler
import webby.mvc.script.watcher.{FileExtTransform, TargetFileTransform}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * Сервер, умеющий собирать множество js-скриптов в один для dev версии. Также, он используется
  * для сборки ресурсов в production.
  * Для dev-closure версии умеет запускать google closure compiler для этих скриптов.
  * Может работать как сервер, так и как компилятор.
  * Для того, чтобы создать этот класс, следует использовать [[GoogleClosure.serverBuilder]]
  *
  * Пример использования этого класса см. в описании метода [[GoogleClosure.serverBuilder]].
  * Для запуска сервера в режиме сборки ресурсов для production, см. метод [[GoogleClosure.runAdvancedCompiler]].
  *
  * Requires sbt dependency
  * {{{
  *   deps += "com.google.javascript" % "closure-compiler" % "v20170124"
  * }}}
  *
  * @param libSource        Исходник библиотеки google closure library. Может быть как в jar-архиве, так и простыми файлами.
  * @param jsSourceDirs     Каталоги, в которых лежат js-исходники проекта для компиляции.
  * @param preCompilers     Список компиляторов, которые должны быть вызываны для каждого исходного файла,
  *                         прежде чем он уйдёт в gcc. Например, здесь могут быть coffeescript, jade компиляторы.
  * @param prepends         В самое начало результирующих файлов gcc будут подставлены эти исходники.
  *                         Если мы собираем dev версию, то будут подставлены полные исходники [[JsSourcePair.source]],
  *                         а для prod версии будет использоваться minified [[JsSourcePair.minified]].
  *                         Например, здесь можно использовать jQuery.
  * @param externs          Gcc externs - специально составленные js файлы, задающие исключения при обработке компилятором.
  *                         Например, [[GoogleClosure.jQueryExtern]]
  * @param targetDir        Каталог, где будут храниться скомпилированные через [[preCompilers]]
  *                         промежуточные версии скриптов.
  * @param targetGccDir     Каталог, куда будут сохраняться скомпилированные gcc финальные скрипты.
  * @param remapEntryPoints Переназначение входных файлов (entry points). Например, если у нас есть
  *                         класс MainEntryPoint.hx, а мы хотим, чтобы он был как main.js, поэтому
  *                         маппинг будет таким: Map("main.js" -> "MainEntryPoint.js")
  * @param sourceMapConfig  Configuration for source map generation
  * @param muteGCCWarnings  Turn off all Google Closure Compiler warnings. Useful with haxe-js compiler.
  */
class GoogleClosureServer(libSource: GoogleClosureLibSource,
                          jsSourceDirs: Seq[Path],
                          preCompilers: List[ScriptCompiler],
                          prepends: Seq[JsSourcePair],
                          externs: Seq[SourceFile],
                          targetDir: Path,
                          targetGccDir: Path,
                          remapEntryPoints: Map[String, String] = Map.empty,
                          errorRenderer: ScriptErrorRenderer = new DefaultScriptErrorRenderer,
                          sourceMapConfig: Option[GccSourceMapConfig] = None,
                          muteGCCWarnings: Boolean = false) {

  val log = webby.api.Logger(getClass)

  def restModulePrepend: String = "if(!window._rest){_restm=[];_rest=function(m){_restm.push(m)}};_rest(function(){"
  def restModuleAppend: String = "})"
  def restModuleWrapper(source: String): String = restModulePrepend + source + restModulePrepend

  private val closureCompiler = new GoogleClosureCompiler(
    externs = externs,
    mainModulePrepends = prepends.map(_.minified),
    resultDir = targetGccDir,
    commonIncludes = Seq(libSource.baseJs),
    sourceMapConfig = sourceMapConfig,
    restModuleWrapper = restModuleWrapper,
    muteAllWarnings = muteGCCWarnings)

  def targetContentType: String = "application/x-javascript"
  private val allowedExtensions: Vector[String] = (Set(".js") ++ preCompilers.map(_.sourceDotExt)).toVector
  private val canonicalSourceDirs: Seq[Path] = jsSourceDirs.map(canonicalize)
  private val canonicalTargetDir: Path = canonicalize(targetDir)

  private val lazyDotExts: Vector[String] = preCompilers.withFilter(_.lazyCompiler).map(_.sourceDotExt)(scala.collection.breakOut)

  private val reverseRemapEntryPoints: Map[String, String] = remapEntryPoints.map(_.swap)

  private var baseJsBody: String = null

  case class ClosureFile(source: SourceFile,
                         provides: Iterable[String],
                         requires: Iterable[String],
                         ignoreMainDeps: Option[String] = None,
                         entryPoint: Boolean = false,
                         lastModified: Long = 0)

  /**
    * Этот класс отличается от [[ClosureFile]] тем, что такой файл не компилируется при каждом
    * проходе [[lazyUpdateFiles()]] по всем внутренним каталогам. Эти файлы компилируются только
    * тогда, когда юзер явно их запросил. Они являются точками входа (entry points).
    * Также, при изменении любого вложенного файла, все entry points становятся устаревшими, и их
    * следует пересобрать при следующем обращении.
    */
  case class LazyClosureFile(sourceDir: Path,
                             localPathStr: String,
                             targetPath:Path,
                             lastModified: Long) {
    def make(): ClosureFile =
      makeClosureFile(isPureJsFile = false, sourceDir = sourceDir, localPathStr = localPathStr, bodyPath = targetPath, sourceModified = lastModified)
  }

  val fileMap = mutable.Map.empty[String, ClosureFile]
  val lazyFileMap = mutable.Map.empty[String, LazyClosureFile]
  var classMap: Map[String, ClosureFile] = _

  private def makeClassMap(): Map[String, ClosureFile] =
    (for {closureFile <- fileMap.values
          cls <- closureFile.provides
    } yield (cls, closureFile)) (scala.collection.breakOut)

  private class DepsParser {
    val regex = "goog\\.addDependency\\((.*)\\);.*".r
    val mapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)

    /**
      * Этот метод не только возвращает maxTimestamp, но и заполняет дерево зависимостей [[fileMap]]
      */
    def parse: Long = {
      var maxTimestamp = 0L
      for (line <- Source.fromString(libSource.depsJs.getCode).getLines()) {
        line match {
          case regex(params) =>
            val tree = mapper.readTree("[" + params + "]")
            val path = tree.get(0).asText()
            val provides: Iterable[JsonNode] = tree.get(1).asScala
            val requires: Iterable[JsonNode] = tree.get(2).asScala
            val cf = ClosureFile(libSource.forPath(path), provides = provides.map(_.asText()), requires = requires.map(_.asText()))
            fileMap.update(cf.source.getName, cf)
            val modified: Long = 1
            if (modified > maxTimestamp) maxTimestamp = modified
          case _ => ()
        }
      }
      maxTimestamp
    }
  }

  /**
    * Вернуть цепочку зависимости, либо причину по которой цепочки не существует.
    * Пример использования:
    * {{{
    *   // в методе serve()
    *   println(printDepTree("adm_js", "goog.async.run"))
    * }}}
    *
    * @param fromName   Класс отправной точки, обычно это entryPoint
    * @param targetName Целевой класс, путь до которого и нужно просчитать
    */
  def printDepTree(fromName: String, targetName: String): Either[String, List[String]] = {
    if (!classMap.contains(fromName)) return Left(s"No fromName:$fromName in deps")
    if (!classMap.contains(targetName)) return Left(s"No targetName:$targetName in deps")
    def recurse(name: String, path: List[String]): Option[List[String]] = {
      if (name == targetName) Some(path)
      else {
        for (req <- classMap(name).requires) {
          recurse(req, path :+ req).foreach(r => return Some(r))
        }
        None
      }
    }
    recurse(fromName, List(fromName)) match {
      case Some(path) => Right(path)
      case None => Left(s"No dependencies from $fromName to $targetName")
    }
  }

  private val closureClassNameMatcher = CharMatcher.javaLetterOrDigit().or(CharMatcher.anyOf("._"))

  private def parseClosureFile(source: SourceFile, lastModified: Long, compiledJsPath: Path) = {
    val googProvide = "goog.provide("
    val googRequire = "goog.require("
    val ignoreMainDepsStr = "// ignoreMainDeps "
    val entryPointStr = "// scriptEntryPoint"
    val providesBuilder = Vector.newBuilder[String]
    val requiresBuilder = Vector.newBuilder[String]
    var ignoreMainDeps: Option[String] = None
    var entryPoint = false
    def validate(cls: String): String = {
      if (!closureClassNameMatcher.matchesAllOf(cls)) sys.error(s"Invalid class name '$cls'")
      cls
    }

    for (gotLine <- source.getCode.linesWithSeparators) {
      val line = StringUtils.stripStart(gotLine, null)
      if (line.startsWith(googProvide)) {
        val closeParenIdx: Int = line.indexOf(')')
        require(closeParenIdx != -1, "Error goog.provide on line: " + line)
        providesBuilder += validate(line.substring(googProvide.length + 1, closeParenIdx - 1))
      } else if (line.startsWith(googRequire)) {
        val closeParenIdx: Int = line.indexOf(')')
        require(closeParenIdx != -1, "Error goog.require on line: " + line)
        requiresBuilder += validate(line.substring(googRequire.length + 1, closeParenIdx - 1))
      } else if (line.startsWith(ignoreMainDepsStr))
        ignoreMainDeps = Some(validate(line.substring(ignoreMainDepsStr.length).trim))
      else if (line.startsWith(entryPointStr))
        entryPoint = true
    }
    new ClosureFile(source, provides = providesBuilder.result(), requires = requiresBuilder.result(),
      lastModified = lastModified, ignoreMainDeps = ignoreMainDeps, entryPoint = entryPoint)
  }

  private def autoCompile(sourceDir: Path, localPath: String): Path = autoCompile(sourceDir, localPath, preCompilers)

  /**
    * Автоматическая компиляция скрипта, используя список компиляторов #compilersLeft.
    * Например, исходный скрипт может быть в формате CoffeeScript, тогда здесь он компилируется в js,
    * и сохраняется в [[canonicalTargetDir]].
    * Компиляция здесь ленивая. Исходный скрипт компилируется только один раз. Для перекомпиляции,
    * его скомпилированная версия должна устареть (проверяем по lastModifiedTime), либо её нужно удалить.
    *
    * @param sourceDir     Исходный каталог скриптов
    * @param localPath     Путь до скрипта внутри исходного каталога
    * @param compilersLeft Список компиляторов, которые могут скомпилировать скрипт
    * @return Возвращает путь к скомпилированному файлу. Он может быть как в #sourceDir (если компиляция не требуется),
    *         так и в [[canonicalTargetDir]], если была компиляция.
    */
  @tailrec
  private def autoCompile(sourceDir: Path, localPath: String, compilersLeft: List[ScriptCompiler]): Path = {
    compilersLeft match {
      case Nil => sourceDir.resolve(localPath)
      case jsCompiler :: tail =>
        localPath match {
          // Компилируем только те скрипты, расширения файлов которых есть в списке #compilersLeft
          case _ if localPath.endsWith(jsCompiler.sourceDotExt) =>
            sourceDir.resolve(localPath) match {
              case path if Files.exists(path) =>
                val filePath: String = canonicalize(path).toString
                if (!filePath.startsWith(sourceDir.toString)) {
                  log.error(s"File '$filePath' not in sourceDir '$sourceDir'")
                  throw ResultException(Results.BadRequest("File not in sourceDir"))
                } else {
                  val targetPath = TargetFileTransform(sourceDir, canonicalTargetDir, jsCompiler.targetFileExt).transformToPath(filePath, reverseRemapEntryPoints)
                  var compileErrors: String = null
                  def checkRecompile(): Boolean = {
                    synchronized {
                      if (!Files.exists(targetPath) || Files.getLastModifiedTime(targetPath).compareTo(Files.getLastModifiedTime(path)) < 0) {
                        val t0 = System.currentTimeMillis()
                        val compileResult = jsCompiler.compileFile(path, targetPath)
                        val t1 = System.currentTimeMillis()
                        if (compileResult.isLeft) {
                          compileErrors = compileResult.left.get
                          errorRenderer.renderToConsole(log, path, compileErrors)
                          return false
                        }
                        val localPathJs = new FileExtTransform(jsCompiler.targetFileExt).transform(localPath)
                        val compiledInfo: String = reverseRemapEntryPoints.get(localPathJs).fold(localPath)(localPath + " => " + _)
                        log.info("Compiled " + compiledInfo + " in " + (t1 - t0) + " ms")
                      }
                    }
                    true
                  }

                  if (checkRecompile()) {
                    targetPath
                  } else {
                    throw ResultException(errorRenderer.renderToClient(path, compileErrors))
                  }
                }
              case _ => autoCompile(sourceDir, localPath, tail)
            }
          case _ => autoCompile(sourceDir, localPath, tail)
        }
    }
  }

  private def makeClosureFile(isPureJsFile: Boolean, sourceDir: Path, localPathStr: String, bodyPath: Path, sourceModified: Long): ClosureFile = {
    if (!isPureJsFile) autoCompile(sourceDir, localPathStr)
    parseClosureFile(SourceFile.builder().withOriginalPath(localPathStr).buildFromFile(bodyPath.toString), sourceModified, bodyPath)
  }

  private def readAndPatchBaseJs(): String = {
    val body = libSource.baseJs.getCode
    // Заменим функцию goog.require заглушкой, чтобы она не подключала сторонние файлы
    body.replaceStd(
      "goog.define('goog.ENABLE_DEBUG_LOADER', true);",
      "goog.define('goog.ENABLE_DEBUG_LOADER', false);")
  }

  /**
    * Максимальное время модификации среди всех исходных файлов. Выставляется в методе [[lazyUpdateFiles()]].
    * Нужен, чтобы определить, что исходные файлы были изменены и требуется перекомпиляция.
    */
  private var maxModified: Long = 0L

  /**
    * Пройтись по всем файлам в исходных каталогах и отследить их изменение/добавление/удаление.
    * Если файлы поменялись, то прочитать их новое содержимое.
    * При первом запуске этот метод читает все файлы.
    *
    * @return есть ли изменённые файлы с прошлой проверки?
    */
  private def lazyUpdateFiles(): Boolean = synchronized {
    var changed = false

    if (baseJsBody == null) baseJsBody = readAndPatchBaseJs()
    if (fileMap.isEmpty) {
      // Получить не только maxModified, но и заполнить дерево зависимостей здесь
      maxModified = Math.max(maxModified, new DepsParser().parse)
      changed = true
    }
    var maxLazyTargetModified = 0L

    def inDir(sourceDir: Path, dir: Path) {
      require(Files.isDirectory(dir), "Not a directory: " + dir)
      Using(Files.newDirectoryStream(dir))(_.asScala.foreach {sourcePath =>
        if (Files.isDirectory(sourcePath)) inDir(sourceDir, sourcePath)
        else {
          val sourcePathStr = sourcePath.toString
          if (allowedExtensions.exists(e => sourcePathStr.endsWith(e))) {
            val localPathStr = getLocalSourcePath(sourceDir, sourcePathStr)
            val localPathJsStr = new FileExtTransform("js").transform(localPathStr)
            val remappedPathJs = reverseRemapEntryPoints.getOrElse(localPathJsStr, localPathJsStr)
            val targetPath = canonicalTargetDir.resolve(remappedPathJs)
            val isPureJsFile = sourcePathStr.endsWith(".js")
            var changeFile = false
            if (!fileMap.contains(sourcePathStr) && !lazyFileMap.contains(sourcePathStr)) {
              // Если добавлен новый файл, или это первичная инициализация класса
              changeFile = true
            } else {
              if (isPureJsFile) {
                if (lastModified(sourcePath) > fileMap(sourcePathStr).lastModified) {
                  // Еcли обновился js файл, и его не нужно конвертировать
                  changeFile = true
                }
              } else {
                if (!Files.exists(targetPath) || Files.getLastModifiedTime(sourcePath).compareTo(Files.getLastModifiedTime(targetPath)) > 0) {
                  // Если обновился файл, который нужно сконвертировать (coffeescript, jade)
                  changeFile = true
                }
              }
            }
            if (changeFile) {
              // Файл изменён. Обновить данные о нём
              val isLazyFile: Boolean = {
                val fileName = sourcePath.getFileName.toString
                lazyDotExts.exists(fileName.endsWith)
              }
              val bodyPath: Path = if (isPureJsFile) sourcePath else targetPath
              val sourceModified: Long = lastModified(sourcePath)
              if (sourceModified > maxModified) maxModified = sourceModified

              val mapKey = sourceDir.resolve(localPathJsStr).toString
              if (!isLazyFile) {
                val closureFile: ClosureFile = makeClosureFile(isPureJsFile, sourceDir, localPathStr, bodyPath, sourceModified)
                fileMap.update(mapKey, closureFile)
              } else {
                if (Files.exists(targetPath)) {
                  val targetModified = lastModified(targetPath)
                  if (targetModified > maxLazyTargetModified) maxLazyTargetModified = targetModified
                }
                lazyFileMap.update(mapKey, LazyClosureFile(sourceDir, localPathStr, targetPath, sourceModified))
                fileMap.remove(mapKey)
              }

              changed = true
            }
          }
        }
      })
    }
    canonicalSourceDirs.foreach(dir => inDir(dir, dir))
    // Удалить все закешированные записи lazyFileMap из fileMap, если они были сгенерированы до последнего изменения.
    if (maxLazyTargetModified < maxModified) {
      for ((key, lazyFile) <- lazyFileMap) {
        fileMap.remove(key)
        Files.deleteIfExists(lazyFile.targetPath)
      }
    }

    if (changed) classMap = makeClassMap()
    changed
  }

  private def getLocalSourcePath(sourceDir: Path, sourcePath: String): String = {
    if (sourcePath.startsWith(sourceDir.toString)) {
      sourcePath.substring(sourceDir.toString.length + 1)
    } else {
      sys.error(s"Path $sourcePath not in source dir")
    }
  }

  private var lastResultLength: Int = 0

  /**
    * Обработать входящий запрос на js, который нужно собрать из множества исходных скриптов.
    * Это девелоперская версия, сам closure compiler здесь не запускается.
    */
  def serveDev(servePath: String)(implicit req: RequestHeader): PlainResult = {
    composeDev(servePath) match {
      case Some(result) => makeResult(result.etag, result.lastModified)(result.result.getBytes)
      case None => Results.NotFoundRaw("File not found")
    }
  }

  case class ComposeDevResult(result: String, etag: String, lastModified: Long)

  def composeDev(servePath: String): Option[ComposeDevResult] = {
    //    val t0 = System.currentTimeMillis()
    lazyUpdateFiles()

    //    val t1 = System.currentTimeMillis()
    var totalCount = 0
    var lastModified = 0L
    val result = new SB(lastResultLength + 1024) {
      val isRest = servePath.endsWith("_rest.js")
      if (isRest) +restModulePrepend + '\n'
      val included = mutable.Set.empty[String]
      def addFile(closureFile: ClosureFile) {
        val name: String = closureFile.source.getName
        if (!included.contains(name)) {
          closureFile.ignoreMainDeps.foreach(n => ignoreJsWithDeps(classMap(n)))
          if (closureFile.entryPoint) {
            prepends.foreach(+_.source.getCode)
            +baseJsBody
          }
          included += name
          for (cls <- closureFile.requires) {
            classMap.get(cls) match {
              case Some(file) => addFile(file)
              case None => log.error("Cannot find class " + cls + "\n  in " + name)
            }
          }
          +"// ------------- " + name + " -------------\n"
          +closureFile.source.getCode + "\n"

          totalCount += 1
          if (closureFile.lastModified > lastModified) lastModified = closureFile.lastModified
        }
      }
      def ignoreJsWithDeps(closureFile: ClosureFile): Unit = {
        included += closureFile.source.getName
        closureFile.requires.foreach {n =>
          ignoreJsWithDeps(classMap.get(n).getOrElse(sys.error("Dependency not found " + n + " in " + closureFile)))
        }
      }
      val remappedPath: String = remapEntryPoints.getOrElse(servePath, servePath)
      tryEntryPoint(remappedPath).foreach(addFile)
      if (isRest) +"\n" + restModuleAppend
    }.toString

    if (totalCount > 0) {
      lastResultLength = result.length

      //    val t2 = System.currentTimeMillis()
      //println("Update time: " + (t1 - t0) + "ms, glue time: " + (t2 - t1) + "ms")

      val etag = lastResultLength + "-" + totalCount
      Some(ComposeDevResult(result, etag, lastModified))
    } else {
      None
    }
  }

  /**
    * Попробовать найти точку входа по всем исходным js каталогам.
    * Также, если файл точки входа не является чистым js, то здесь мы его сначала соберём, а
    * потом вернём [[ClosureFile]] для него.
    * @param servePath Путь к файлу относительно [[jsSourceDirs]]
    * @return Скомпилированный [[ClosureFile]] (если такой найдётся)
    */
  private def tryEntryPoint(servePath: String): Option[ClosureFile] = {
    canonicalSourceDirs.foreach {sourceDir =>
      val sourcePath: Path = sourceDir.resolve(servePath)
      val key = sourcePath.toString
      fileMap.get(key).orElse {
        lazyFileMap.get(key).map {f =>
          val compiledClosureFile = f.make()
          fileMap.update(key, compiledClosureFile)
          compiledClosureFile
        }
      }.foreach(f => return Some(f))
    }
    None
  }

  /**
    * Обработать входящий запрос на js, который нужно собрать из множества исходных скриптов в один
    * файл, скомилированный closure compiler в режиме advanced.
    * Это тоже девелоперская версия, но выходной js получается максимально близкий к тому, который
    * будет на продакшне.
    */
  def serveClosureCompiled(servePath: String)(implicit req: RequestHeader): PlainResult = synchronized {
    if (!servePath.endsWith(".js") || servePath.contains("/")) Results.NotFoundRaw("File not found")
    else {
      val fullModule = StringUtils.removeEnd(servePath, ".js")
      val (module: String, isRest: Boolean) =
        if (fullModule.endsWith("_rest")) (StringUtils.removeEnd(fullModule, "_rest"), true)
        else (fullModule, false)

      val compiledPath = targetGccDir.resolve(fullModule + ".js")
      lazyUpdateFiles()
      val needRecompile: Boolean = !Files.exists(compiledPath) || lastModified(compiledPath) < maxModified
      if (needRecompile) {
        log.info("Compiling js closure module \"" + module + "\"")
        externs.foreach(_.clearCachedSource()) // Externs могли обновиться, поэтому сбросить кеш для них
        printCompiledResult(compileClosureModule(module))
      }
      val resultPath = targetGccDir.resolve(module + (if (isRest) "_rest.js" else ".js"))
      if (Files.exists(resultPath)) {
        val modified = lastModified(resultPath)
        makeResult(modified.toString, modified)(Files.readAllBytes(resultPath))
      } else Results.InternalServerError("Compile error")
    }
  }

  /**
    * Запустить компиляцию нескольких модулей Google closure compiler'ом
    */
  def compileClosure(modules: Seq[String]): Unit = {
    lazyUpdateFiles()
    modules.par.foreach {module =>
      printCompiledResult(compileClosureModule(module))
    }
  }

  /**
    * Запустить компиляцию одного модуля Google closure compiler'ом
    */
  private def compileClosureModule(module: String): Seq[Path] = {
    val moduleJs = module + ".js"
    val remappedModuleJs = remapEntryPoints.getOrElse(moduleJs, moduleJs)
    val mainClosureFile: ClosureFile = tryEntryPoint(remappedModuleJs)
      .getOrElse(sys.error("File for module \"" + module + "\" does not exist. Using sourceDirs: " + canonicalSourceDirs))

    val restModule = module + "_rest"
    val restModuleJs = restModule + ".js"
    val remappedRestModuleJs = remapEntryPoints.getOrElse(restModuleJs, restModuleJs)
    val restClosureFile = tryEntryPoint(remappedRestModuleJs)

    val mainDeps: Iterable[SourceFile] = getDepsFor(mainClosureFile)
    val restDeps: Iterable[SourceFile] = restClosureFile match {
      case Some(cf) => getDepsFor(cf)
      case None => Nil
    }
    closureCompiler.compileModule(module, mainDeps, restDeps)
  }

  private class JsDepsAggregator {
    val included = mutable.Set[String]()
    val includedSources = ArrayBuffer[SourceFile]()

    def addFile(closureFile: ClosureFile) {
      val name = closureFile.source.getName
      if (!included.contains(name)) {
        closureFile.ignoreMainDeps.foreach(n => ignoreJsWithDeps(classMap(n)))
        included += name
        includedSources += closureFile.source
        for (cls <- closureFile.requires) {
          addFile(classMap.getOrElse(cls, sys.error(s"Cannot find module '$cls' in file $name")))
        }
      }
    }

    private def ignoreJsWithDeps(closureFile: ClosureFile): Unit = {
      if (included.add(closureFile.source.getName))
        includedSources += closureFile.source
      closureFile.requires.foreach(n => ignoreJsWithDeps(classMap(n)))
    }
  }

  private def getDepsFor(closureFile: ClosureFile): Iterable[SourceFile] = {
    val agg = new JsDepsAggregator
    agg.addFile(closureFile)
    agg.includedSources
  }

  private def printCompiledResult(resultFiles: Seq[Path]): Unit = {
    resultFiles.foreach {p => log.info(p.getFileName + " " + (Files.size(p) / 1024) + " kb")}
  }

  /**
    * Выдать результат с учётом хедеров if-none-match, if-modified-since.
    */
  private def makeResult(etag: String, lastModified: Long)(result: => Array[Byte])(implicit req: RequestHeader): PlainResult = {
    if (req.headers.get(HttpHeaders.IF_NONE_MATCH).contains(etag) &&
      req.headers.get(HttpHeaders.IF_MODIFIED_SINCE).contains(lastModified.toString)) {
      PlainResult(HttpResponseStatus.NOT_MODIFIED)
    } else {
      ScriptServer.maybeGzippedResult(result)
        .withHeader(HttpHeaders.CONTENT_TYPE, targetContentType)
        .withHeader(HttpHeaders.LAST_MODIFIED, StdDates.httpDateFormatMillis(lastModified))
        .withHeader(HttpHeaders.ETAG, etag)
        .withHeader(HttpHeaders.CACHE_CONTROL, "no-cache")
    }
  }

  private def lastModified(path: Path): Long = Files.getLastModifiedTime(path).toMillis

  private def canonicalize(path: Path): Path = path.toAbsolutePath.normalize
}
