import sbt.Keys._
import sbt._

object WebbySbtPluginBuild extends Build {
  lazy val root = Project(
    "sbt-plugin",
    file("."),
    settings = Defaults.coreDefaultSettings ++ Seq(
      organization := "com.github.citrum.webby",
      version := "0.1-SNAPSHOT",

      ivyLoggingLevel := UpdateLogging.DownloadOnly,
      publishTo := {
        val nexus = "http://nexus/"
        if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "content/repositories/releases")
      },
      credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
      publishMavenStyle := true,
      publishArtifact in Compile := true,

      sources in doc in Compile := List(),
      javacOptions ++= Seq("-encoding", "UTF-8"),
      scalacOptions ++= Seq("-target:jvm-1.7", "-encoding", "UTF-8", "-Xlint", "-deprecation", "-unchecked", "-feature"),
      scalaVersion := "2.10.6",

      sbtPlugin := true,

      sourceDirectory in Compile <<= baseDirectory(_ / "src"),
      scalaSource in Compile <<= baseDirectory(_ / "src"),
      javaSource in Compile <<= baseDirectory(_ / "src")

      // version, sbtVersion, scalaVersion смотреть по наличию каталога в http://repo.typesafe.com/typesafe/releases/com/github/mpeltonen/
      //libraryDependencies += "com.github.mpeltonen" % "sbt-idea" % "1.5.2" extra("sbtVersion" -> "0.13", "scalaVersion" -> "2.10"),
      //      unmanagedJars in Compile <++= (baseDirectory) map { b => sbtJars(b / "../..") },
      //      publishArtifact in packageDoc := buildWithDoc,
    )
  )
}
