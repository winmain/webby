package webby.api.db.evolutions

import java.io.File
import java.sql.{Connection, PreparedStatement}
import javax.annotation.Nullable

import org.slf4j.LoggerFactory
import webby.api._
import webby.api.db.DbPlugin
import webby.api.mvc.{Action, Resultable}
import webby.commons.io.{IOUtils, Url}
import webby.commons.text.StringWrapper.wrapper
import webby.html.WebbyPage
import webby.mvc.AppStub

import scala.collection.immutable.IntMap

/**
  * Database evolution support.
  * Example usage:
  * {{{
  *   object DbEvolutions extends WebbyDbEvolutions(new DbLocalRootConfigPlugin)
  * }}}
  *
  * @param createRootDbPlugin            Factory to create [[DbPlugin]] with root privileges
  *                                      to execute SQL patches
  * @param evolutionsSubDir              Relative directory where evolution SQL files located
  * @param dbVersionManagerFullTableName Full table name (optionally with database and schema)
  *                                      to store evolution version in Database.
  *                                      Set to null to store version in a file.
  * @param fileVersionManager            Relative directory to store version in a file. Set to null
  *                                      to store version in a database. Not recommended.
  * @param autoExecuteInProd             Flag to execute SQL patches automatically on a server
  *                                      start via [[EvolutionsPlugin]] in a production profile.
  * @param admConf                       Configuration to setup admin controller (optionally).
  *                                      If set then the controller will be initialized in
  *                                      [[WebbyDbEvolutions#admEvolutionsCtl]].
  */
class WebbyDbEvolutions(createRootDbPlugin: => DbPlugin,
                        evolutionsSubDir: String = "conf/db_evolutions",
                        @Nullable dbVersionManagerFullTableName: String = "db_evolution",
                        @Nullable fileVersionManager: String = null, // "db-evolution-version"
                        autoExecuteInProd: Boolean = true,
                        @Nullable val admConf: DbEvolutionsAdmConf = null) {
  require(dbVersionManagerFullTableName != null ^ fileVersionManager != null, "Define one of `dbVersionManagerFullTableName`, `fileVersionManager`")

  val log = LoggerFactory.getLogger(getClass.getName.replaceStd("$", ""))
  val withRootDbConnection = new WithRootDbConnection(createRootDbPlugin)
  def evolutionsDir = App.app.path.resolve(evolutionsSubDir).toFile

  def vm: VersionManager = {
    if (dbVersionManagerFullTableName != null) new DbVersionManager(log, withRootDbConnection, dbVersionManagerFullTableName)
    else new FileVersionManager(log, App.app.path.resolve(fileVersionManager).toFile)
  }

  val admEvolutionsCtl: AdmEvolutionsCtl = if (admConf != null) new AdmEvolutionsCtl(this, admConf) else null

  /**
    * Некоторые проверки в именах файлов эволюций.
    * Если найдены новые эволюции, то сообщить об этом в лог.
    */
  def checkEvolutions() {
    var numbers = IntMap.empty[Unit]
    val currentVersion = vm.getCurrentVersion
    var newEvolutions = 0
    for (name <- getEvolutionFiles) {
      val idx: Int = name.indexOf('.')
      if (idx == -1) sys.error("Invalid db evolution name: " + name)
      val number = name.substring(0, idx).toInt
      if (numbers.contains(number)) sys.error("Duplicate db evolution number: " + name)
      numbers = numbers.updated(number, Unit)
      if (number > currentVersion) newEvolutions += 1
    }
    if (newEvolutions > 0) log.info(s"::: There are $newEvolutions new ${if (newEvolutions == 1) "evolution" else "evolutions"}.")
  }

  /**
    * Производим эволюцию БД, т.е., выполняем запросы
    */
  def updateEvolutions(): Boolean = {
    val toUpdateEvolutions = getNewEvolutions(vm.getCurrentVersion)

    // IntMap всегда выдаёт записи, сортированные по ключу
    if (toUpdateEvolutions.nonEmpty) {
      val uniquePattern = "\uffff\ufffe\ufff0"
      for ((num, evFile) <- toUpdateEvolutions) {
        withRootDbConnection.use {c =>
          c.setAutoCommit(false)
          log.info("Applying evolution {}", num)
          var evBody = IOUtils.readString(evFile)
          // Удалить комментарии
          evBody = evBody.splitChar('\n').filterNot(s => s.startsWith("#") || s.startsWith("--")).mkString("\n")
          for (rawSql <- evBody.replaceStd(";;", uniquePattern).splitByWholeSeparator(";\n")) {
            val sql = rawSql.replaceStd(uniquePattern, ";").trim
            if (sql.nonEmpty) {
              log.info("::: Executing: {}", sql)
              val st: PreparedStatement = c.prepareStatement(sql)
              st.execute()
              st.getUpdateCount match {
                case affectedRows if affectedRows > 0 => log.info(":::    Affected rows: {}", affectedRows)
                case _ =>
              }
            }
          }
          c.commit()
        }
        vm.saveCurrentVersion(num)
      }
      log.info("Finished updating evolutions")
      true
    } else {
      false
    }
  }

  /**
    * Вернуть эволюции, новее currentVersion
    */
  def getNewEvolutions(currentVersion: Int): IntMap[File] = {
    var toUpdateEvolutions = IntMap.empty[File]
    val baseDir: File = evolutionsDir
    for (name <- getEvolutionFiles) {
      val idx: Int = name.indexOf('.')
      val number = name.substring(0, idx).toInt
      if (number > currentVersion) {
        toUpdateEvolutions = toUpdateEvolutions.updated(number, new File(baseDir, name))
      }
    }
    toUpdateEvolutions
  }

  private def getEvolutionFiles: Seq[String] = evolutionsDir.list.view.filter(name => !name.startsWith("-") && name.endsWith(".sql"))

  /**
    * При запуске из командной строки делаем обновление.
    */
  def main(args: Array[String]) {
    AppStub.withAppNoPluginsDev {
      val result = updateEvolutions()
      if (!result) log.info("No new evolutions")
    }
  }

  // ------------------------------- EvolutionsPlugin -------------------------------

  /**
    * Поддержка эволюций БД.
    * Для локалки: производим некоторые проверки в именах файлов эволюций
    * Для jenkins: производим эволюцию БД, т.е., выполняем запросы
    */
  class EvolutionsPlugin(app: Application) extends Plugin {
    override def enabled: Boolean = !app.profile.isTest
    override def onStart() {
      if (App.isJenkins || (autoExecuteInProd && App.isProd) || vm.getCurrentVersion == 0) {
        try updateEvolutions()
        catch {case e: Exception => log.error("Error applying evolutions", e); throw e}
      } else if (App.isDev) {
        checkEvolutions()
      }
    }
  }
}

case class DbEvolutionsAdmConf(applyWrapper: (WebbyPage => Resultable) => Action,
                               admMainUrl: Url,
                               evolutionApplyNewUrl: Url)

class WithRootDbConnection(createRootDbPlugin: => DbPlugin) {
  def use[R](block: Connection => R): R = {
    val plugin = createRootDbPlugin
    plugin.onStart()
    val conn: Connection = plugin.getConnection
    try block(conn)
    finally {
      conn.close()
      plugin.onStop()
    }
  }
}
