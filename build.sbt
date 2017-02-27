import sbt.Keys.{baseDirectory, ivyLoggingLevel, packageBin, scalaSource, sources, startYear, _}
import sbt.{UpdateLogging, _}
import haxeidea.HaxeLib._

val buildScalaVersion = "2.12.1"

val baseSettings = _root_.bintray.BintrayPlugin.bintrayPublishSettings ++ Seq(
  organization := "com.github.citrum.webby",
  version := "0.4.0-SNAPSHOT",

  incOptions := incOptions.value.withNameHashing(nameHashing = true),
  resolvers ++= Seq(
    "zeroturnaround repository" at "https://repos.zeroturnaround.com/nexus/content/repositories/zt-public/", // The zeroturnaround.com repository
    Resolver.bintrayRepo("citrum", "maven") // Repo for querio
  ),

  sources in doc in Compile := List(), // Выключить генерацию JavaDoc, ScalaDoc
  mainClass in Compile := None,
  ivyLoggingLevel := UpdateLogging.DownloadOnly,

  // Deploy settings
  startYear := Some(2016),
  homepage := Some(url("https://github.com/citrum/webby")),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  bintrayVcsUrl := Some("https://github.com/citrum/webby"),
  bintrayOrganization := Some("citrum")
)

val commonSettings = baseSettings ++ Seq(
  scalaVersion := buildScalaVersion,
  crossScalaVersions := Seq("2.11.8", "2.12.1"),

  scalacOptions ++= Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature", "-language:existentials"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8"),
  javacOptions in doc := Seq("-source", "1.8"),
  ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true)) // forcing scala version
)

// Минимальный набор зависимостей
val commonDependencies = {
  val deps = Seq.newBuilder[ModuleID]
  deps += "ch.qos.logback" % "logback-classic" % "1.2.1" // Логирование
  deps += "org.apache.commons" % "commons-lang3" % "3.5"
  deps += "com.google.guava" % "guava" % "21.0"
  deps += "com.google.code.findbugs" % "jsr305" % "3.0.1" // @Nonnull, @Nullable annotation support
  deps += "commons-io" % "commons-io" % "2.5" // Содержит полезные классы типа FileUtils

  // Tests
  deps += "org.scalatest" %% "scalatest" % "3.0.1" % "test"
  deps += "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test"

  deps.result()
}
val querio = "com.github.citrum.querio" %% "querio" % "0.6.7" // querio orm

/**
  * Создать список настроек, задающих стандартные пути исходников, ресурсов, тестов для проекта.
  */
def makeSourceDirs(): Seq[Setting[_]] = Seq(
  sourceDirectories in Compile += baseDirectory.value / "src",
  scalaSource in Compile := baseDirectory.value / "src",
  javaSource in Compile := baseDirectory.value / "src",
  resourceDirectory in Compile := baseDirectory.value / "conf",
  scalaSource in Test := baseDirectory.value / "test",
  resourceDirectory in Test := baseDirectory.value / "test-conf")

/**
  * Запустить scala класс кодогенерации в отдельном процессе
  */
def runScala(classPath: Seq[File], className: String, arguments: Seq[String] = Nil) {
  val ret: Int = new Fork("java", Some(className)).apply(ForkOptions(bootJars = classPath, outputStrategy = Some(StdoutOutput)), arguments)
  if (ret != 0) sys.error("Trouble with code generator")
}

// ------------------------------ elastic-orm project ------------------------------

lazy val elasticOrm: Project = Project(
  "elastic-orm",
  file("elastic-orm"),
  settings = Defaults.coreDefaultSettings ++ commonSettings ++ makeSourceDirs() ++ Seq(
    libraryDependencies ++= commonDependencies,
    libraryDependencies += "org.elasticsearch" % "elasticsearch" % "2.2.0" exclude("com.google.guava", "guava"), // Клиент поискового движка (да и сам движок), exclude guava нужен потому что эластик использует более старую версию 18
    libraryDependencies += querio
  )).dependsOn(webby)

// ------------------------------ webby-haxe project ------------------------------

lazy val webbyHaxeBuild: Project = Project(
  "webby-haxe-build",
  file("webby-haxe/build"),
  settings = baseSettings ++ haxeLibSettings ++ Seq(
    name := "webby-haxe",
    artifactPath := baseDirectory.value / "webby-haxe.jar",

    sourceDirectories in Compile := Seq(baseDirectory.value / "../src", baseDirectory.value / "../macro")

    // TODO: add haxe build task before deploy
  )
)

// ------------------------------ webby project ------------------------------

lazy val webby: Project = Project(
  "webby",
  file("webby"),
  settings = commonSettings ++ makeSourceDirs() ++ Seq(
    description := "Webby is a scala web framework",
    libraryDependencies := {
      val deps = Seq.newBuilder[ModuleID]
      deps ++= commonDependencies
      deps += "org.slf4j" % "jul-to-slf4j" % "1.7.24"
      deps += "org.slf4j" % "jcl-over-slf4j" % "1.7.24"

      deps += "io.netty" % "netty-all" % "4.1.2.Final"

      deps += "com.typesafe" % "config" % "1.3.0"

      // Важно! Нельзя повышать версию модуля jackson-module-scala на ветку 2.5, 2.6, 2.7.
      // Это приводит к смене поведения при сериализации. Например, webby.form.jsrule.JsRule
      // перестаёт сериализовывать свойства cond, actions несмотря на аннотации @JsonProperty.
      // Если же не ставить @JsonAutoDetect(getterVisibility = NONE), то сериализация работает, хотя
      // появляются лишние поля.
      deps += "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.7" // Работа с json
      deps += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.7" exclude("com.google.guava", "guava") exclude("com.google.code.findbugs", "jsr305") // Работа с json
      deps += "com.intellij" % "annotations" % "12.0" // для интеграции IDEA language injection

      // Optional dependencies
      deps += querio % "optional" // Querio ORM
      deps += "com.typesafe.akka" %% "akka-actor" % "2.4.17" % "optional" // Used in webby.api.libs.concurrent.Akka
      deps += "com.typesafe.akka" %% "akka-slf4j" % "2.4.17" % "optional"
      deps += "org.scala-stm" %% "scala-stm" % "0.8" % "optional" // Used in webby.api.libs.concurrent.Promise
      deps += "com.zaxxer" % "HikariCP" % "2.4.7" % "optional" // Database connector, used in webby.api.db.HikariCPPlugin
      deps += "org.jsoup" % "jsoup" % "1.6.3" % "optional" // Html parsing, used in webby.commons.text.StdStrHtmlJsoup
      deps += "org.zeroturnaround" % "jr-sdk" % "6.4.6" % "optional" // JRebel SDK (class reloader), used in webby.commons.system.JRebelUtils
      deps += "uk.co.caprica" % "juds" % "0.94.1" % "optional" // Unix socket support, used in webby.commons.system.SdDaemon
      deps += "javax.servlet" % "javax.servlet-api" % "3.1.0" % "optional" // Servlet api for dump Sentry client
      deps += "com.getsentry.raven" % "raven-logback" % "7.3.0" % "optional" exclude("com.google.guava", "guava") // Sentry plugin for log processing. Guava excluded because of old version 18 used by raven. Used in webby.commons.system.log.SentryFilteredAppender
      deps += "commons-validator" % "commons-validator" % "1.5.1" % "optional" intransitive() // Email validation, used in webby.commons.text.validator.EmailValidator
      deps += "org.apache.commons" % "commons-email" % "1.4" % "optional" // Email classes, used in webby.commons.text.validator.EmailValidator
      deps += "org.quartz-scheduler" % "quartz" % "2.2.3" % "optional" exclude("c3p0", "c3p0") // Cron, used in webby.commons.system.cron.BaseQuartzPlugin
      deps += "commons-codec" % "commons-codec" % "1.10" % "optional"
      deps += "net.sf.ehcache" % "ehcache-core" % "2.6.11" % "optional" // Cache, used in webby.commons.cache.CachePlugin
      deps += "com.esotericsoftware.kryo" % "kryo" % "2.24.0" % "optional" // For serializing objects in cache, used in webby.commons.cache.KryoNamedCache
      deps += "com.carrotsearch" % "hppc" % "0.7.1" % "optional" // High Performance Primitive Collections, used in ElasticSearch & in webby.commons.cache.IntIntPositiveValueMap
      deps += "com.google.javascript" % "closure-compiler" % "v20170124" % "optional" exclude("com.google.guava", "guava") // Google Closure Compiler
      deps += "org.clojure" % "google-closure-library" % "0.0-20160609-f42b4a24" % "optional" // Google Closure Library
      deps += "org.glassfish.external" % "opendmk_jmxremote_optional_jar" % "1.0-b01-ea" % "optional" // JMXMP - better replacement for RMI
      deps.result()
    },
    javacOptions ++= Seq("-XDenableSunApiLintControl"),
    scalacOptions ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-Xlint", "-Xlint:-nullary-unit") // nullary-unit нужен только для оператора def > : Unit в CommonTag
  )
)

lazy val root = Project(
  "webby-root",
  file("."),
  aggregate = Seq(webby, elasticOrm, webbyHaxeBuild),
  settings = Seq(
    // Disable packaging & publishing artifact
    Keys.`package` := file(""),
    publishArtifact := false,
    publishLocal := {},
    publish := {},
    bintrayUnpublish := {},

    // Наводим красоту в командной строке sbt
    shellPrompt := {state: State => "[" + scala.Console.GREEN + "webby" + scala.Console.RESET + "] "}
  ))
