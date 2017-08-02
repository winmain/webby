package webby.api.db.evolutions
import java.io.File
import java.sql.{Connection, SQLException}

import org.slf4j.Logger
import webby.commons.io.{IOUtils, Using}

trait VersionManager {
  def getCurrentVersion: Int
  def saveCurrentVersion(v: Int): Unit
}


class FileVersionManager(log: Logger, file: File) extends VersionManager {
  override def getCurrentVersion: Int = {
    if (file.exists())
      IOUtils.readString(file).trim.toInt
    else {
      log.warn("No evolution file found: " + file + ", starting from zero")
      0
    }
  }
  override def saveCurrentVersion(v: Int): Unit = {
    IOUtils.writeToFile(file, v.toString)
  }
}


class DbVersionManager(log: Logger,
                       withRootDbConnection: WithRootDbConnection,
                       fullTableName: String) extends VersionManager {
  def versionField = "version"

  override def getCurrentVersion: Int =
    withRootDbConnection.use(getCurrentVersion0) match {
      case None =>
        log.warn("No evolution table found: " + fullTableName + ", starting from zero")
        0
      case Some(v) => v
    }

  override def saveCurrentVersion(v: Int): Unit = {
    withRootDbConnection.use {c =>
      try setVersion0(c, v)
      catch {
        case _: SQLException =>
          // Automatically create new table on error and try to save version one more time
          log.info(s"Creating evolution table $fullTableName")
          createTable(c)
          setVersion0(c, v)
      }
    }
  }

  private def getCurrentVersion0(c: Connection): Option[Int] = {
    try
      Using(c.prepareStatement(s"select $versionField from $fullTableName")) {st =>
        Using(st.executeQuery()) {result =>
          if (result.next()) Some(result.getInt(1))
          else None
        }
      }
    catch {
      case _: SQLException => None
    }
  }

  private def setVersion0(c: Connection, v: Int): Unit = {
    val updateCount = Using(c.prepareStatement(s"update $fullTableName set $versionField = $v"))(_.executeUpdate())
    if (updateCount == 0) {
      Using(c.prepareStatement(s"insert into $fullTableName ($versionField) values ($v)"))(_.executeUpdate())
    } else if (updateCount != 1) {
      log.warn("Update count must be 1, but was " + updateCount +
        " while saving evolution version. Something weird happened.")
    }
  }

  private def createTable(c: Connection): Unit = {
    Using(c.prepareStatement(s"create table $fullTableName ($versionField integer not null)")) {st =>
      st.execute()
    }
  }
}
