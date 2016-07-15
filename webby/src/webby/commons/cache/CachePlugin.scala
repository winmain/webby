package webby.commons.cache

import net.sf.ehcache.CacheManager
import webby.api.{App, Application, Plugin}

/**
  * Плагин, использующий ehcache.
  *
  * Requires sbt dependencies
  * {{{
  *   deps += "net.sf.ehcache" % "ehcache-core" % "2.6.11"
  * }}}
  */
class CachePlugin(app: Application) extends Plugin {

  lazy val manager: CacheManager = {
    app.configuration.getString("cache.config") match {
      case Some(configUrl) =>
        Thread.currentThread().getContextClassLoader.getResourceAsStream(configUrl) match {
          case null => sys.error("Resource cache.config=" + configUrl + " not found")
          case stream => CacheManager.create(stream)
        }
      case None => sys.error("Config file need to be specified in cacheplugin.config")
    }
  }

  override def onStart() {
    if (App.isProd) manager
  }

  override def onStop() {
    manager.shutdown()
  }

  // Плагин обычно укладывается в 400мс, поэтому увеличим ему время на остановку без логирования.
  override def minTimeToLogInfoOnStop: Long = 1000L
}
