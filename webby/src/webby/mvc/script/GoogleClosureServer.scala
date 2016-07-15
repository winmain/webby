package webby.mvc.script

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.CharMatcher
import com.google.common.net.HttpHeaders
import compiler.ScriptCompiler
import io.netty.handler.codec.http.HttpResponseStatus
import org.apache.commons.lang3.StringUtils
import watcher.{FileExtTransform, TargetFileTransform}
import webby.api.mvc.{PlainResult, RequestHeader, ResultException, Results}
import webby.commons.collection.IterableWrapper.wrapIterable
import webby.commons.date.StdDates
import webby.commons.io.IOUtils
import webby.commons.text.SB
import webby.commons.text.StringWrapper.wrapper

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source

/**
  * Сервер, умеющий собирать множество js-скриптов в один для dev версии. Также, он используется
  * для сборки ресурсов в production.
  * Для dev-closure версии умеет запускать google closure compiler для этих скриптов.
  * Может работать как сервер, так и как компилятор.
  *
  * Пример использования этого класса:
  * {{{
  * object GoogleClosureServers {
  *   def create(profile: String, targetClosureCompiledDir: Path): GoogleClosureServer = {
  *     val isDev: Boolean = App.isDevOrJenkins
  *     val closureLibDir = Paths.root.resolve("script/closure-library")
  *     new GoogleClosureServer(
  *       closureBasePath = Paths.root.resolve("public/js/google-closure/base.js"),
  *       closureDepsPath = Paths.root.resolve("public/js/google-closure/deps.js"),
  *       jsSourceDir = Paths.assets.resolve("js"),
  *       jsSourceProfileDir = Paths.assets.resolve("profiles/" + profile),
  *       jsDepsAliases = Vector("../../../~closure-library/" -> closureLibDir),
  *       jsDepsIgnore = Vector("../../../assets/"),
  *       compilers = List(
  *         ExternalCoffeeScriptCompiler(goog = true),
  *         ExternalJadeClosureCompiler(noDebug = !isDev, pretty = isDev)),
  *       prependJQuery = Some(Paths.public.resolve(Public.jQueryPath)),
  *       externFiles = Vector(
  *         Paths.public.resolve("js/externs/jquery-1.9.js"),
  *         Paths.assets.resolve("js/lib/externs.js")),
  *       jQueryPrependMinifiedPath = Paths.public.resolve(Public.jQueryMinPath),
  *       closureLibDir = closureLibDir,
  *       targetDir = Paths.targetAssets.resolve("js"),
  *       targetClosureCompiledDir = targetClosureCompiledDir
  *     )
  *   }
  *   def create(profile: String): GoogleClosureServer =
  *     create(profile, targetClosureCompiledDir = Paths.targetAssets.resolve("js-closure-compiled"))
  * }
  * }}}
  *
  * Специальный объект для компиляции js google closure compiler'ом в production.
  * Далее, этот объект (GoogleClosureAdvancedCompiler) вызывается при сборке production ресурсов.
  * {{{
  * object GoogleClosureAdvancedCompiler {
  *   def main(args: Array[String]) {
  *     require(args.length == 1, "Must be one arg: profile name")
  *     println("--- Google Closure Compiler step ---")
  *     val t0 = System.currentTimeMillis()
  *     AppStub.withAppNoPluginsDev {
  *       val server = GoogleClosureServer.create(args(0),
  *         targetClosureCompiledDir = Paths.root.resolve("target/assets-release/js-prod"))
  *       server.compileClosure(Seq("main", "mobile", "adm"))
  *     }
  *     val t1 = System.currentTimeMillis()
  *     println("--- Google Closure Compiler finished in " + (t1 - t0) + " ms ---")
  *   }
  * }
  * }}}
  *
  * Requires sbt dependency
  * {{{
  *   deps += "com.google.javascript" % "closure-compiler" % "v20160619"
  * }}}
  */
class GoogleClosureServer(closureBasePath: Path,
                          closureDepsPath: Path,
                          jsSourceDir: Path,
                          jsSourceProfileDir: Path,
                          jsDepsAliases: Iterable[(String, Path)],
                          jsDepsIgnore: Iterable[String],
                          compilers: List[ScriptCompiler],
                          prependJQuery: Option[Path],
                          externFiles: Seq[Path],
                          jQueryPrependMinifiedPath: Path,
                          closureLibDir: Path,
                          targetDir: Path,
                          targetClosureCompiledDir: Path) {

  val log = webby.api.Logger(getClass)

  def restModulePrepend: String = "if(!window._rest){_restm=[];_rest=function(m){_restm.push(m)}};_rest(function(){"
  def restModuleAppend: String = "})"
  def restModuleWrapper(source: String): String = restModulePrepend + source + restModulePrepend

  private val closureCompiler = new GoogleClosureCompiler(
    externFiles = externFiles,
    jQueryPrependPath = jQueryPrependMinifiedPath,
    resultDir = targetClosureCompiledDir,
    commonIncludes = Seq(closureBasePath.toAbsolutePath.toString),
    restModuleWrapper = restModuleWrapper)

  def targetContentType: String = "application/x-javascript"
  private val allowedExtensions: Vector[String] = Vector(".js", ".coffee", ".jade")
  private val canonicalSourceDir: Path = canonicalize(jsSourceDir)
  private val canonicalSourceProfileDir: Path = canonicalize(jsSourceProfileDir)
  private val canonicalTargetDir: Path = canonicalize(targetDir)
  private val canonicalDepsAliases: Iterable[(String, Path)] = jsDepsAliases.map(a => a._1 -> canonicalize(a._2))

  private var baseJsBody: String = null
  private var jQueryBody: String = null

  case class ClosureFile(path: Path,
                         provides: Iterable[String],
                         requires: Iterable[String],
                         compiledJsPath: Path,
                         ignoreMainDeps: Option[String] = None,
                         entryPoint: Boolean = false,
                         var body: String = null,
                         lastModified: Long = 0) {
    //    val isInSourceDir: Boolean = file.getPath.startsWith(canonicalSourceDir.getPath)
    //    def localPath: String = if (isInSourceDir) file.getPath.substring(canonicalSourceDir.getPath.length + 1) else null
  }
  val fileMap = mutable.Map.empty[String, ClosureFile]
  var classMap: Map[String, ClosureFile] = _

  def makeClassMap(): Map[String, ClosureFile] =
    (for {closureFile <- fileMap.values
          cls <- closureFile.provides
    } yield (cls, closureFile)) (scala.collection.breakOut)

  class DepsParser(pathAliases: Iterable[(String, Path)], pathIgnore: Iterable[String]) {
    val regex = "goog\\.addDependency\\((.*)\\);.*".r
    val mapper = new ObjectMapper().configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)

    def parse(depsPath: Path): Long = {
      var maxTimestamp = 0L
      for (line <- Source.fromFile(depsPath.toFile).getLines()) {
        line match {
          case regex(params) =>
            val tree = mapper.readTree("[" + params + "]")
            val path = tree.get(0).asText()
            val provides = tree.get(1)
            val requires = tree.get(2)
            if (!pathIgnore.exists(alias => path.startsWith(alias))) {
              val pathPath: Path = pathAliases.find(alias => path.startsWith(alias._1)) match {
                case Some((prefix, basePath)) => canonicalize(basePath.resolve(path.substring(prefix.length)))
                case None => sys.error(s"Unknown alias for path '$path' in google closure deps.js file")
              }
              val absPath: Path = pathPath.toAbsolutePath
              val cf = ClosureFile(absPath, provides = provides.map(_.asText()), requires = requires.map(_.asText()),
                compiledJsPath = absPath)
              fileMap.update(pathPath.toAbsolutePath.toString, cf)
              val modified: Long = lastModified(pathPath)
              if (modified > maxTimestamp) maxTimestamp = modified
            }
          case _ => ()
        }
      }
      maxTimestamp
    }
  }

  def saveDeps() {
    IOUtils.writeToFile(closureDepsPath, new SB {
      for (classPath <- fileMap.keys.toVector.sorted) {
        val closurePath = fileMap(classPath)
        val path = closurePath.path.toString
        val publicPath = canonicalDepsAliases.find(alias => path.startsWith(alias._2.toString)) match {
          case Some((prefix, basePath)) => prefix + path.substring(basePath.toString.length + 1)
          case None => sys.error(s"Unknown alias for path '$path' while creating google closure deps.js file")
        }
        +"goog.addDependency('" + publicPath + "', " + "["
        closurePath.provides.foreachWithSep(+"'" + _ + "'", +", ")
        +"], ["
        closurePath.requires.foreachWithSep(+"'" + _ + "'", +", ")
        +"]);\n"
      }
    }.toString)
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

  private val closureClassNameMatcher = CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.anyOf("._"))

  private def parseClosureFile(sourcePath: Path, body: String, lastModified: Long, compiledJsPath: Path) = {
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

    for (gotLine <- body.linesWithSeparators) {
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
    new ClosureFile(sourcePath, provides = providesBuilder.result(), requires = requiresBuilder.result(),
      compiledJsPath = compiledJsPath, body = body, lastModified = lastModified, ignoreMainDeps = ignoreMainDeps, entryPoint = entryPoint)
  }

  private def autoCompile(localPath: String): Path = autoCompile(localPath, compilers)

  @tailrec
  private def autoCompile(localPath: String, compilersLeft: List[ScriptCompiler]): Path =
    compilersLeft match {
      case Nil => canonicalSourceDir.resolve(localPath)
      case jsCompiler :: tail =>
        localPath match {
          case _ if localPath.endsWith(jsCompiler.sourceDotExt) =>
            canonicalSourceDir.resolve(localPath) match {
              case path if Files.exists(path) =>
                val filePath: String = canonicalize(path).toString
                if (!filePath.startsWith(canonicalSourceDir.toString)) {
                  log.error(s"File '$filePath' not in sourceDir '$canonicalSourceDir'")
                  throw ResultException(Results.BadRequest("File not in sourceDir"))
                } else {
                  val targetPath = TargetFileTransform(canonicalSourceDir, canonicalTargetDir, jsCompiler.targetFileExt).transformToPath(filePath)
                  def checkRecompile(): Boolean = {
                    synchronized {
                      if (!Files.exists(targetPath) || Files.getLastModifiedTime(targetPath).compareTo(Files.getLastModifiedTime(path)) < 0) {
                        val t0 = System.currentTimeMillis()
                        val compileResult = jsCompiler.compileFile(path, targetPath)
                        val t1 = System.currentTimeMillis()
                        if (compileResult.isLeft) {
                          log.error("Error compiling:\n" + compileResult.left.get)
                          return false
                        }
                        log.info("Compiled " + localPath + " in " + (t1 - t0) + " ms")
                      }
                    }
                    true
                  }

                  if (checkRecompile()) {
                    targetPath
                    //                    SimpleResult(HttpResponseStatus.OK, Resource.fromFile(targetFile).byteArray)
                    //                      .withHeader(HttpHeaders.Names.CONTENT_TYPE, compiler.targetContentType)
                  } else {
                    throw ResultException(Results.InternalServerError("Compilation error"))
                  }
                }
              case _ => autoCompile(localPath, tail)
            }
          //          case _ if compiler.sourceMapDotExt.exists(localPath.endsWith) => new File(canonicalTargetDir, localPath)
          case _ => autoCompile(localPath, tail)
        }
    }

  def readAndPatchBaseJs(): String = {
    val body = IOUtils.readString(closureBasePath)
    // Заменим функцию goog.require заглушкой, чтобы она не подключала сторонние файлы
    body.replaceStd(
      "goog.define('goog.ENABLE_DEBUG_LOADER', true);",
      "goog.define('goog.ENABLE_DEBUG_LOADER', false);")
  }

  /**
    * Максимальное время модификации среди всех исходных файлов. Выставляется в методе [[lazyUpdateFiles()]].
    * Нужен, чтобы определить, что исходные файлы были изменены и требуется перекомпиляция.
    */
  var maxModified: Long = 0L

  /**
    * Пройтись по всем файлам в исходных каталогах и отследить их изменение/добавление/удаление.
    * Если файлы поменялись, то прочитать их новое содержимое.
    * При первом запуске этот метод читает все файлы.
    *
    * @return есть ли изменённые файлы с прошлой проверки?
    */
  def lazyUpdateFiles(): Boolean = synchronized {
    var changed = false

    if (baseJsBody == null) baseJsBody = readAndPatchBaseJs()
    if (jQueryBody == null) jQueryBody = prependJQuery.fold("")(IOUtils.readString)
    if (fileMap.isEmpty) {
      maxModified = Math.max(maxModified, new DepsParser(canonicalDepsAliases, jsDepsIgnore).parse(closureDepsPath))
      changed = true
    }

    def inDir(sourceDir: Path) {
      require(Files.isDirectory(sourceDir), "Not a directory: " + sourceDir)
      resource.managed(Files.newDirectoryStream(sourceDir)).foreach(_.foreach {sourcePath =>
        if (Files.isDirectory(sourcePath)) inDir(sourcePath)
        else if (allowedExtensions.exists(e => sourcePath.toString.endsWith(e))) {
          val localPathStr = getLocalSourcePath(sourcePath.toString)
          val targetPath = canonicalTargetDir.resolve(new FileExtTransform("js").transform(localPathStr))
          val isPureJsFile = sourcePath.toString.endsWith(".js")
          var changeFile = false
          if (!fileMap.contains(sourcePath.toString)) {
            // Если добавлен новый файл, или это первичная инициализация класса
            changeFile = true
          } else {
            if (isPureJsFile) {
              if (lastModified(sourcePath) > fileMap(sourcePath.toString).lastModified) {
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
            val bodyPath: Path = if (isPureJsFile) sourcePath else targetPath
            if (!isPureJsFile) autoCompile(localPathStr)
            val modified: Long = lastModified(bodyPath)
            if (modified > maxModified) maxModified = modified
            val closureFile = parseClosureFile(sourcePath, IOUtils.readString(bodyPath), modified, bodyPath)
            fileMap.update(sourcePath.toString, closureFile)
            changed = true
          }
        }
      })
    }
    inDir(canonicalSourceDir)
    inDir(canonicalSourceProfileDir)

    if (changed) classMap = makeClassMap()
    changed
  }

  def getLocalSourcePath(sourcePath: String): String = {
    if (sourcePath.startsWith(canonicalSourceDir.toString)) {
      sourcePath.substring(canonicalSourceDir.toString.length + 1)
    } else if (sourcePath.startsWith(canonicalSourceProfileDir.toString)) {
      sourcePath.substring(canonicalSourceProfileDir.toString.length + 1)
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
        val path: String = closureFile.path.toString
        if (!included.contains(path)) {
          closureFile.ignoreMainDeps.foreach(n => ignoreJsWithDeps(classMap(n)))
          if (closureFile.entryPoint) {
            +jQueryBody
            +baseJsBody
          }
          included += path
          for (cls <- closureFile.requires) {
            classMap.get(cls) match {
              case Some(file) => addFile(file)
              case None => log.error("Cannot find class " + cls + "\n  in " + path)
            }
          }
          // Прочитать содержимое файла из google library
          if (closureFile.body == null) closureFile.body = IOUtils.readString(closureFile.path)

          +"// ------------- " + path + " -------------\n"
          +closureFile.body + "\n"

          totalCount += 1
          if (closureFile.lastModified > lastModified) lastModified = closureFile.lastModified
        }
      }
      def ignoreJsWithDeps(closureFile: ClosureFile): Unit = {
        included += closureFile.path.toString
        closureFile.requires.foreach {n =>
          ignoreJsWithDeps(classMap.get(n).getOrElse(sys.error("Dependency not found " + n + " in " + closureFile)))
        }
      }
      val sourcePath: Path = canonicalSourceDir.resolve(servePath)
      fileMap.get(sourcePath.toString).foreach(addFile)
      if (isRest) +"\n" + restModuleAppend
    }.toString

    lastResultLength = result.length

    //    val t2 = System.currentTimeMillis()
    //println("Update time: " + (t1 - t0) + "ms, glue time: " + (t2 - t1) + "ms")

    val etag = lastResultLength + "-" + totalCount
    makeResult(etag, lastModified)(result.getBytes)
  }

  /**
    * Обработать входящий запрос на js, который нужно собрать из множества исходных скриптов в один
    * файл, скомилированный closure compiler в режиме advanced.
    * Это тоже девелоперская версия, но выходной js получается максимально близкий к тому, который
    * будет на продакшне.
    */
  def serveClosureCompiled(servePath: String)(implicit req: RequestHeader): PlainResult =
    synchronized {
      if (!servePath.endsWith(".js") || servePath.contains("/")) Results.NotFoundRaw
      else {
        val fullModule = StringUtils.removeEnd(servePath, ".js")
        val (module: String, isRest: Boolean) =
          if (fullModule.endsWith("_rest")) (StringUtils.removeEnd(fullModule, "_rest"), true)
          else (fullModule, false)

        val compiledPath = targetClosureCompiledDir.resolve(fullModule + ".js")
        lazyUpdateFiles()
        val needRecompile: Boolean = !Files.exists(compiledPath) || lastModified(compiledPath) < maxModified
        if (needRecompile) {
          log.info("Compiling js closure module \"" + module + "\"")
          printCompiledResult(compileClosureModule(module))
        }
        val resultPath = targetClosureCompiledDir.resolve(module + (if (isRest) "_rest.js" else ".js"))
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
    val sourcePath: Path = canonicalSourceDir.resolve(module + ".js")
    val sourceRestPath: Path = canonicalSourceDir.resolve(module + "_rest.js")
    require(Files.exists(sourcePath), "File for module " + sourcePath.toAbsolutePath + " does not exist")

    val mainDeps: Iterable[String] = getDepsFor(sourcePath.toString)
    val restDeps: Iterable[String] = if (Files.exists(sourceRestPath)) getDepsFor(sourceRestPath.toString) else Nil
    closureCompiler.compileModule(module, mainDeps, restDeps)
  }

  class JsDepsAggregator() {
    val included = mutable.Set.empty[String]
    def addFile(closureFile: ClosureFile) {
      val path: String = closureFile.compiledJsPath.toString
      if (!included.contains(path)) {
        closureFile.ignoreMainDeps.foreach(n => ignoreJsWithDeps(classMap(n)))
        included += path
        for (cls <- closureFile.requires) {
          addFile(classMap.getOrElse(cls, sys.error(s"Cannot find module '$cls' in file $path")))
        }
      }
    }
    private def ignoreJsWithDeps(closureFile: ClosureFile): Unit = {
      included += closureFile.compiledJsPath.toString
      closureFile.requires.foreach(n => ignoreJsWithDeps(classMap(n)))
    }
  }

  private def getDepsFor(jsFilePath: String): Iterable[String] = {
    val agg = new JsDepsAggregator
    fileMap.get(jsFilePath).foreach(agg.addFile)
    agg.included
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
