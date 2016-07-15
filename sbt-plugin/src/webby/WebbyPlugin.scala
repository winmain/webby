package webby
import sbt.Keys._
import sbt._

object WebbyPlugin extends AutoPlugin {
  object autoImport extends WebbyAutoImport

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements
  override def projectSettings = webbyProjectSettings
}
