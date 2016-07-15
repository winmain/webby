import sbt.Keys._
import sbt._

object WebbyBuild extends Build {
  val buildScalaVersion = "2.11.8"

  val commonSettings = Seq(
    organization := "webby",
    version := "0.1-SNAPSHOT",

    scalaVersion := buildScalaVersion,

    incOptions := incOptions.value.withNameHashing(nameHashing = true),
    resolvers ++= Seq(
      "Local Nexus" at "http://nexus/content/groups/public",
      "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases"),
    sources in doc in Compile := List(), // Выключить генерацию JavaDoc, ScalaDoc
    scalacOptions ++= Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature", "-language:existentials"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8"),
    javacOptions in doc := Seq("-source", "1.8"),
    // Эта опция для более быстрого обновления зависимостей sbt
//    updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
    ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)), // forcing scala version
    ivyLoggingLevel := UpdateLogging.DownloadOnly,
    publishMavenStyle := false,
    mainClass in Compile := None
  )

  // Минимальный набор зависимостей
  val commonDependencies = {
    val deps = Seq.newBuilder[ModuleID]
    deps += "ch.qos.logback" % "logback-classic" % "1.1.7" // Логирование
    deps += "org.apache.commons" % "commons-lang3" % "3.4"
    deps += "com.google.guava" % "guava" % "19.0"
    deps += "com.google.code.findbugs" % "jsr305" % "3.0.1" // @Nonnull, @Nullable annotation support
    deps += "net.sourceforge.jregex" % "jregex" % "1.2_01" // Быстрый regexp, поддерживающий синтаксис perl5.6 и incomplete matching
    deps += "com.jsuereth" %% "scala-arm" % "1.4" // Automatic resource management, autoclose files
    deps += "commons-io" % "commons-io" % "2.5" // Содержит полезные классы типа FileUtils

    // Tests
    deps += "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test"
    deps += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"

    deps.result()
  }

  val querioVersion = "0.5.2"
  val querio = "com.github.winmain" %% "querio" % querioVersion exclude("com.google.code.findbugs", "jsr305") // querio orm
  val useQuerioRoot = false
  lazy val QuerioProject = RootProject(file("../querio"))
  lazy val QuerioProjectCodegen = ProjectRef(file("../querio"), "querio-codegen")
  implicit class _ProjectWrapper(proj: Project) {
    def dependsOnQuerio(): Project = if (useQuerioRoot) proj.dependsOn(QuerioProject) else proj.settings(libraryDependencies += querio)
    def dependsOnQuerioCodegen(): Project = if (useQuerioRoot) proj.dependsOn(QuerioProjectCodegen) else proj.settings(libraryDependencies += querio)
  }

  /**
    * Создать список настроек, задающих стандартные пути исходников, ресурсов, тестов для проекта.
    */
  private def makeSourceDirs(): Seq[Setting[_]] = Seq(
    sourceDirectories in Compile <+= baseDirectory(_ / "src"),
    scalaSource in Compile <<= baseDirectory(_ / "src"),
    javaSource in Compile <<= baseDirectory(_ / "src"),
    resourceDirectory in Compile <<= baseDirectory(_ / "conf"),
    scalaSource in Test <<= baseDirectory(_ / "test"),
    resourceDirectory in Test <<= baseDirectory(_ / "test-conf"))

  /**
    * Запустить scala класс кодогенерации в отдельном процессе
    */
  private def runScala(classPath: Seq[File], className: String, arguments: Seq[String] = Nil) {
    val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath, outputStrategy = Some(StdoutOutput)), arguments)
    if (ret != 0) sys.error("Trouble with code generator")
  }

  // ------------------------------ elastic-orm project ------------------------------

  lazy val elasticOrm: Project  = Project(
    "elastic-orm",
    file("elastic-orm"),
    settings = Defaults.coreDefaultSettings ++ commonSettings ++ makeSourceDirs() ++ Seq(
      libraryDependencies ++= commonDependencies,
      libraryDependencies += "org.elasticsearch" % "elasticsearch" % "2.2.0" exclude("com.google.guava", "guava") // Клиент поискового движка (да и сам движок), exclude guava нужен потому что эластик использует более старую версию 18
  )).dependsOnQuerio().dependsOn(webby)

  // ------------------------------ routes project ------------------------------

  lazy val routes: Project  = Project(
    "routes",
    file("routes"),
    settings = Defaults.coreDefaultSettings ++ commonSettings ++ makeSourceDirs() ++ Seq(
      libraryDependencies ++= commonDependencies
//      libraryDependencies ++= Seq(jsr305, jregex, commonsLang3)
    )).dependsOn(webby)

  // ------------------------------- script-compiler project -------------------------------

  lazy val scriptCompiler: Project = Project(
    "script-compiler",
    file("script-compiler"),
    settings = Defaults.coreDefaultSettings ++ commonSettings ++ makeSourceDirs() ++ Seq(
      libraryDependencies ++= commonDependencies
    ))

  // ------------------------------ webby project ------------------------------

  lazy val webby: Project = Project(
    "webby",
    file("webby"),
    settings = commonSettings ++ makeSourceDirs() ++ Seq(
      libraryDependencies := {
        val deps = Seq.newBuilder[ModuleID]
        deps ++= commonDependencies
        deps += "org.slf4j" % "jul-to-slf4j" % "1.7.21"
        deps += "org.slf4j" % "jcl-over-slf4j" % "1.7.21"

        deps += "io.netty" % "netty-all" % "4.1.2.Final"

        deps += "com.typesafe" % "config" % "1.3.0"

        // Важно! Нельзя повышать версию модуля jackson-module-scala на ветку 2.5, 2.6, 2.7.
        // Это приводит к смене поведения при сериализации. Например, lib.form.jsrule.JsRule
        // перестаёт сериализовывать свойства cond, actions несмотря на аннотации @JsonProperty.
        // Если же не ставить @JsonAutoDetect(getterVisibility = NONE), то сериализация работает, хотя
        // появляются лишние поля.
        deps += "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.5" // Работа с json
        deps += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.5-1" exclude("com.google.guava", "guava") exclude("com.google.code.findbugs", "jsr305") // Работа с json
        deps += "com.fasterxml.jackson.module" % "jackson-module-jaxb-annotations" % "2.4.5" // Нужен для форка jackson-dataformat-xml
        deps += "com.intellij" % "annotations" % "12.0" // для интеграции IDEA language injection

        // Optional dependencies
        deps += "com.typesafe.akka" %% "akka-actor" % "2.4.8" % "optional" // Used in webby.api.libs.concurrent.Akka
        deps += "com.typesafe.akka" %% "akka-slf4j" % "2.4.8" % "optional"
        deps += "org.scala-stm" %% "scala-stm" % "0.7" % "optional" // Used in webby.api.libs.concurrent.Promise
        deps += "com.zaxxer" % "HikariCP" % "2.4.7" % "optional" // Database connector, used in webby.api.db.HikariCPPlugin
        deps += "org.jsoup" % "jsoup" % "1.6.3" % "optional" // Html parsing, used in webby.commons.text.StdStrHtmlJsoup
        deps += "de.xam" % "md5" % "2.6.3" % "optional" // Used in webby.commons.codec.MD5
        deps += "org.zeroturnaround" % "jr-sdk" % "6.4.6" % "optional" // JRebel SDK (class reloader), used in webby.commons.system.JRebelUtils
        deps += "uk.co.caprica" % "juds" % "0.94.1" % "optional" // Unix socket support, used in webby.commons.system.SdDaemon
        deps += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "optional" // Servlet api for dump Sentry client
        deps += "com.getsentry.raven" % "raven-logback" % "7.3.0" % "optional" exclude("com.google.guava", "guava") // Sentry plugin for log processing. Guava excluded because of old version 18 used by raven. Used in webby.commons.log.SentryFilteredAppender
        deps += "commons-validator" % "commons-validator" % "1.5.1" % "optional" intransitive() // Email validation, used in webby.commons.validator.EmailValidator
        deps += "org.apache.commons" % "commons-email" % "1.4" % "optional" // Email classes, used in webby.commons.validator.EmailValidator
        deps += "org.quartz-scheduler" % "quartz" % "2.2.3" % "optional" exclude("c3p0", "c3p0") // Cron, used in webby.commons.cron.BaseQuartzPlugin
        deps += "commons-codec" % "commons-codec" % "1.10" % "optional"
        deps += "net.sf.ehcache" % "ehcache-core" % "2.6.11" % "optional" // Cache, used in webby.commons.cache.CachePlugin
        deps += "com.esotericsoftware.kryo" % "kryo" % "2.24.0" % "optional" // For serializing objects in cache, used in webby.commons.cache.KryoNamedCache
        deps += "com.carrotsearch" % "hppc" % "0.7.1" % "optional" // High Performance Primitive Collections, used in ElasticSearch & in webby.commons.cache.IntIntPositiveValueMap
        deps += "com.github.winmain" %% "querio" % querioVersion % "optional" exclude("com.google.code.findbugs", "jsr305") // Querio ORM
        deps += "com.google.javascript" % "closure-compiler" % "v20160619" % "optional" // Google Closure Compiler
        deps.result()
      },
//      unmanagedClasspath in Compile += (baseDirectory in root).value / "unmanaged/log-utils/build",

      javacOptions ++= Seq("-XDenableSunApiLintControl"),
      scalacOptions ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-Xlint", "-Xlint:-nullary-unit") // nullary-unit нужен только для оператора def > : Unit в CommonTag
//      publishArtifact in packageDoc := buildWithDoc,
//      publishArtifact in (Compile, packageSrc) := true,
//      parallelExecution in Test := false

      // Добавить скомпилированные классы из папки unmanaged в сборку финального jar файла
//      mappings in (Compile, packageBin) ++= {
//        (((baseDirectory in root).value / "unmanaged/log-utils/build" ** "*") filter {_.isFile})
//          .get pair relativeTo((baseDirectory in root).value / "unmanaged/log-utils/build")
//      }
    )
  ).dependsOn(scriptCompiler) //.dependsOn(SbtSharedProject)

  lazy val root = Project(
    "webby-root",
    file("."),
    aggregate = Seq(webby, elasticOrm, routes, scriptCompiler),
    settings = Seq(
      // Disable packaging & publishing artifact
      Keys.`package` := file(""),
      publishArtifact := false,
      publishLocal := {},
      publish := {},

      // Наводим красоту в командной строке sbt
      shellPrompt := {state: State => "[" + scala.Console.GREEN + "webby" + scala.Console.RESET + "] "}
    ))
}
