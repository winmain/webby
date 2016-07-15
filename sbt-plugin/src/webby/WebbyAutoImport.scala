package webby
import sbt.Keys._
import sbt.{File, Project, ProjectRef, ProjectReference, RootProject, _}

abstract class WebbyAutoImport {
  // ------------------------------- Project default settings -------------------------------

  lazy val webbyProjectSettings = Seq[Setting[_]](
    sourceDirectory in Compile <<= baseDirectory(_ / "app"),
    scalaSource in Compile <<= baseDirectory(_ / "app"),
    javaSource in Compile <<= baseDirectory(_ / "app"),

    sourceDirectory in Test <<= baseDirectory(_ / "test"),
    scalaSource in Test <<= baseDirectory(_ / "test"),
    javaSource in Test <<= baseDirectory(_ / "test"),

    resourceDirectory in Compile <<= baseDirectory(_ / "conf"),

    artifactName := ((sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
      "_project-" + artifact.name + artifact.classifier.fold("")("-" + _) + "." + artifact.extension), // Задать имя генерируемого артефакта с префиксом "_project-", чтобы подключить его первым

    unmanagedResourceDirectories in Compile ++= Seq(
      baseDirectory.value / "app",
      target.value / "asset-resources"),
    excludeFilter in unmanagedResources := HiddenFileFilter || "*.scala" || "*.java",

    // Чтобы работал debug в idea для тестов
    // Но с этой опцией (sbt.Keys.fork in Test := false) не работает тест models.user.CompaniesSpec,
    // поэтому я поменял Test => (Test, testOnly), чтобы форк выключался (а дебаг включался) только в задании testOnly.
    sbt.Keys.fork in(Test, testOnly) := false,

    profile := BuildProfile.dev,

    compTask,

    // Наводим красоту в командной строке sbt
    shellPrompt := {state: State =>
      val e = Project.extract(state)
      name.in(e.currentRef).get(e.structure.data).fold("> ")("[" + Colors.cyan(_) + "] ")
    },

    // Запретить параллельное выполнение тестов, и добавить форк потому, что AppStub,
    // который вызывает App.start(), совсем не умеет работать параллельно.
    parallelExecution in Test := false,
    fork in Test := true,

    distExcludes := Nil,
    webbyPackageEverythingTask,
    webbyStageTask,
    mainClass := Some("webby.core.server.NettyServer")
  )

  // ------------------------------- Profile -------------------------------

  // profile you’re going to build against
  case class BuildProfile(name: String)
  object BuildProfile {
    val dev = BuildProfile("dev")
    val jenkins = BuildProfile("jenkins")
    val prod = BuildProfile("prod")
  }
  // setting to make the functionality be accessible from the outside (e.g., the terminal)
  val profile = SettingKey[BuildProfile]("profile", "Uses resources for the specified profile.")

  // ------------------------------- Relative projects -------------------------------

  var relativeProjectFile: (String) => File = {projectName =>
    file(System.getProperty("user.home") + "/ws/" + projectName)
  }

  /**
    * Объявление проекта, который может быть подключен как в виде обычной jar библиотеки, так
    * и в виде отдельного sbt проекта с исходниками, что очень полезно при разработке библиотек.
    *
    * Пример использования:
    * {{{
    * val querio = RelativeProject("querio", "com.github.winmain" %% "querio" % "0.5.2" exclude("com.google.code.findbugs", "jsr305"), useRef = false)
    * val querioCodegen = querio.copy(projectId = "querio-codegen")
    *
    * val webby = RelativeProject("webby", "webby" %% "webby" % "0.1-SNAPSHOT", projectId = "webby", useRef = false)
    * val webbyRoutes = webby.copy(projectId = "routes", moduleId = webby.moduleId.copy(name = "routes"))
    * val webbyElasticOrm = webby.copy(projectId = "elastic-orm", moduleId = webby.moduleId.copy(name = "elastic-orm"))
    * val webbyScriptCompiler = webby.copy(projectId = "script-compiler", moduleId = webby.moduleId.copy(name = "script-compiler"))
    *
    * ...
    * lazy val main: Project = Project(...).dependsOn(querio).aggregate(webbyRoutes)
    * }}}
    * @param name      Название папки с проектом в каталоге workspace (по-дефолту этот каталог задан как ~/ws,
    *                  но его можно переопределить, перезаписав значение переменной relativeProjectFile
    * @param moduleId  Строка подключения проекта обычной библиотекой
    * @param projectId Если задан, то это Project.id, как оно задано в sbt конфиге проекта либы.
    * @param useRef    Самая главная настройка этого класса.
    *                  Если true - то используем проект как [[RootProject]]/[[ProjectRef]],
    *                  т.е. подключаем по исходникам. Если false - то подключаем как обычную либу.
    */
  case class RelativeProject(name: String, moduleId: ModuleID, projectId: String = null, useRef: Boolean = false) {
    def projectFile: File = relativeProjectFile(name)

    val project: ProjectReference =
      if (projectId == null) RootProject(projectFile)
      else ProjectRef(projectFile, projectId)

    def dependsFor(proj: Project): Project =
      if (useRef) proj.dependsOn(project)
      else proj.settings(libraryDependencies += moduleId)
    def aggregate(proj: Project): Project = if (useRef) proj.aggregate(project) else proj
  }
  implicit class _ProjectWrapper(proj: Project) {
    def dependsOn(relativeProject: RelativeProject): Project = relativeProject.dependsFor(proj)
    def aggregate(relativeProject: RelativeProject): Project = relativeProject.aggregate(proj)
    def dependsAndAggregate(relativeProject: RelativeProject): Project = relativeProject.aggregate(relativeProject.dependsFor(proj))
  }

  // ------------------------------- Stage -------------------------------

  val distExcludes = SettingKey[Seq[String]]("dist-excludes")

  def inAllDependencies[T](base: ProjectRef, key: SettingKey[T], structure: BuildStructure): Seq[T] = {
    def deps(ref: ProjectRef): Seq[ProjectRef] =
      Project.getProject(ref, structure).toList.flatMap {p =>
        p.dependencies.map(_.project) ++ p.aggregate
      }
    Dag.topologicalSort(base)(deps).flatMap {p => key in p get structure.data}
  }

  /**
    * Executes the {{packaged-artifacts}} task in the current project (the project to which this setting is applied)
    * and all of its dependencies, yielding a list of all resulting {{jar}} files *except*:
    *
    * * jar files from artifacts with names in [[distExcludes]]
    * * the jar file that is returned by {{packageSrc in Compile}}
    * * the jar file that is returned by {{packageDoc in Compile}}
    *
    * Дополнение: sbt.Keys.`package` запускает задание по упаковке основного jar артефакта со всеми собранными ресурсами.
    * Оно может работать и без этого явно вызванного задания, но нестабильно. По непонятным причинам jenkins периодически
    * собирает неправильный артефакт, без скомилированных классов. Поэтому, я добавил сюда этот таск явно.
    */
  val webbyPackageEverything = TaskKey[Seq[File]]("webby-package-everything")
  lazy val webbyPackageEverythingTask = webbyPackageEverything <<=
    (state, thisProjectRef, distExcludes).flatMap {(state, project, excludes) =>
      def theTask[T](t: TaskKey[T]): SettingKey[Task[T]] = Scoped.scopedSetting(t.scope, t.key)
      def taskInAllDependencies[T](taskKey: TaskKey[T]): Task[Seq[T]] =
        inAllDependencies(project, theTask(taskKey), Project structure state).join

      taskInAllDependencies(packagedArtifacts).flatMap {packaged: Seq[Map[Artifact, File]] =>
        taskInAllDependencies(packageSrc in Compile).flatMap {srcs: Seq[File] =>
          taskInAllDependencies(packageDoc in Compile).map {docs: Seq[File] =>
            val allJars: Seq[Iterable[File]] = for {
              artifacts: Map[Artifact, File] <- packaged
            } yield {
              artifacts
                .filter {case (art, _) => art.extension == "jar" && !excludes.contains(art.name)}
                .map {case (_, path) => path}
            }
            allJars
              .flatten
              .diff(srcs ++ docs) //remove srcs & docs since we do not need them in the dist
              .distinct
          }
        }
      }
    }

  /**
    * Stage task создаёт папку target/staged, в которую складывает все jar-ки, необходимые для
    * запуска проекта на production.
    */
  val webbyStage = TaskKey[Unit]("stage")
  lazy val webbyStageTask = webbyStage := {
    val packaged = webbyPackageEverything.value
    val dependencies = (dependencyClasspath in Runtime).value
    val targetValue = target.value

    val staged = targetValue / "staged"

    IO.delete(staged)
    IO.createDirectory(staged)

    val libs = dependencies.withFilter(_.data.ext == "jar").map(_.data) ++ packaged

    libs.foreach {jar =>
      IO.copyFile(jar, new File(staged, jar.getName))
    }

    mainClass.value match {
      case Some(mainClassValue) =>
        val start = targetValue / "start"
        IO.write(start,
          "#!/usr/bin/env sh\n" +
            "\n" +
            "exec java $@ -cp \"`dirname $0`/staged/*\" " + mainClassValue + " `dirname $0`/..")
        "chmod a+x %s".format(start.getAbsolutePath).!

      case None => streams.value.log.info("No mainClass value defined, so no 'target/start' file created")
    }
  }

  // ------------------------------- Codegens -------------------------------

  /**
    * Специальное задание, облегчающее запуск скриптов кодогенерации из проекта codegen.
    *
    * Как его использовать:
    * 1. В проекте должен быть отдельный модуль codegen, в котором находятся сами кодогенераторы
    * 2. В настройки (settings) проекта codegen добавить codegenRunnerTask
    * 3. Создать свой task, в котором нужно вызвать codegenRunner в контексте проекта codegen
    *
    * Пример:
    * {{{
    * lazy val codegen = Project("codegen", base = file("modules/codegen"),
    *   settings = Defaults.coreDefaultSettings ++ commonSettings ++ makeSourceDirs() ++ Seq(
    *     libraryDependencies ++= commonDependencies,
    *     codegenRunnerTask
    * ))
    *
    * val genDbSources = taskKey[Unit]("gen-db-sources")
    * lazy val genDbSourcesTask = genDbSources := {
    *   (codegenRunner in codegen).value.run("codegen.GenDbSources", (scalaSource in Compile).value.absolutePath)
    * }
    * }}}
    * В этом примере мы объявляем проект codegen и задание genDbSources, которое запускает codegen.GenDbSources.
    */
  val codegenRunner = taskKey[CodegenRunner]("codegen-runner")
  lazy val codegenRunnerTask = codegenRunner := {
    (compile in Compile).value // Run codegen compile task
    val classPath: Seq[File] =
      (dependencyClasspath in Compile).value.files :+
        (baseDirectory in Compile).value :+
        (classDirectory in Runtime).value
    new CodegenRunner(classPath)
  }
  class CodegenRunner(classPath: Seq[File]) {
    def run(className: String, arguments: Seq[String] = Nil): Unit = runScala(classPath, className, arguments)
    def run(className: String, argument1: String, otherArguments: String*): Unit = run(className, argument1 +: otherArguments)
  }

  // ------------------------------- Utilities -------------------------------

  /**
    * Создать список настроек, задающих стандартные пути исходников, ресурсов, тестов для проекта.
    */
  def makeSourceDirs(): Seq[Setting[_]] = Seq(
    sourceDirectories in Compile <+= baseDirectory(_ / "src"),
    scalaSource in Compile <<= baseDirectory(_ / "src"),
    javaSource in Compile <<= baseDirectory(_ / "src"),
    resourceDirectory in Compile <<= baseDirectory(_ / "conf"),
    scalaSource in Test <<= baseDirectory(_ / "test"),
    resourceDirectory in Test <<= baseDirectory(_ / "test-conf"))

  /**
    * Запустить scala класс кодогенерации в отдельном процессе
    */
  def runScala(classPath: Seq[File], className: String, arguments: Seq[String] = Nil) {
    val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath, outputStrategy = Some(StdoutOutput)), arguments)
    if (ret != 0) sys.error("Execution " + className + " ends with error")
  }

  /**
    * Task: Компиляция, показывающая точное время компиляции
    */
  val comp = TaskKey[Unit]("comp")
  lazy val compTask = comp <<=
    (state in Compile) map {state =>
      val t0 = System.currentTimeMillis()
      Project.extract(state).runTask(compile in Compile, state)
      val t1 = System.currentTimeMillis()
      println(Colors.magenta("--- " + (t1 - t0) + " ms"))
    }
}
