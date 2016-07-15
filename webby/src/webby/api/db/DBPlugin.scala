package webby.api.db
import java.sql.Connection

import webby.api.Plugin

/**
 * Generic DBPlugin interface
 */
trait DBPlugin extends Plugin {

  def getConnection: Connection

  def shutdownPool()
}
