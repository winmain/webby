package webby.api

/**
 * A Webby plugin.
 *
 * You can define a Webby plugin this way:
 * {{{
 * class MyPlugin(app: Application) extends Plugin
 * }}}
 *
 * The plugin class must be declared in a webby.plugins file available in the classpath root:
 * {{{
 * 1000:myapp.MyPlugin
 * }}}
 * The associated int defines the plugin priority.
 */
trait Plugin {

  /**
   * Called when the application starts.
   */
  def onStart() {}

  /**
   * Called before application shutdown.
   */
  def onPrepareToShutdown() {}

  /**
   * Called when the application stops.
   */
  def onStop() {}

  /**
   * Is the plugin enabled?
   */
  def enabled: Boolean = true

  /** Минимальное время отработки [[onPrepareToShutdown()]] или [[onStart()]], начиная с которого
    * будет info-запись в webby логе об этом.
    */
  def minTimeToLogInfoOnStop: Long = 300L

  /** Минимальное время отработки [[onPrepareToShutdown()]] или [[onStart()]], начиная с которого
    * будет warn-запись в webby логе об этом.
    */
  def minTimeToLogWarnOnStop: Long = 3000L
}

