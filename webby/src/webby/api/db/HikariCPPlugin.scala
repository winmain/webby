package webby.api.db

import java.sql.{Connection, SQLException}

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import webby.api.App

import scala.util.Try

/**
  * Плагин для подключения [[HikariDataSource]].
  *
  * Это опциональный модуль.
  * Для его использования нужно добавить зависимость hikariCP в проект:
  * {{{
  * deps += "com.zaxxer" % "HikariCP" % "2.4.7"
  * }}}
  */
abstract class HikariCPPlugin extends DbPlugin {
  private var ds: HikariDataSource = _

  override def onStart() {
    val config = getConfig
    ds = new HikariDataSource(config)
    if (isTestConnection) {
      try {
        ds.getConnection.close()
      } catch {
        case e: SQLException =>
          throw new RuntimeException("Plugin: " + getClass.getSimpleName + ", unable to connect to DB", e)
      }
    }
  }

  override def onStop() {
    Try(ds.close()).get
  }

  override def getConnection: Connection = ds.getConnection
  override def shutdownPool() = ds.close()

  def isTestConnection = App.profile.isJenkinsOrProd

  // ------------------------------- Abstract methods -------------------------------

  def getConfig: HikariConfig
}
