package webby.api.db
import java.sql.Connection

import webby.api.Plugin

/**
 * Generic DbPlugin interface
 */
trait DbPlugin extends Plugin {

  def getConnection: Connection

  def shutdownPool()
}
