package webby.mvc

import java.nio.file.{Path, Paths}

import webby.api._
import webby.core.system.{ApplicationProvider, StaticAppProvider, TestAppProvider}

object AppStub {

  val path: Path = Paths.get(".")

  private var app: ApplicationProvider = null
  private var stub = false

  def isStub = stub

  /**
   * Запустить приложение-заглушку (для крона и для тестов)
   */
  def start(profile: Profile, allowPlugins: Boolean = true) = {
    App.maybeApp.filter(app == null || _ != app.get.right.get).foreach(_ => sys.error("Cannot create stub. Another application is running."))
    stub = true
    app = new StaticAppProvider(path, profile, allowPlugins) // App.start(app) вызывается внутри конструктора StaticApplication
    app
  }

  def stop() {
    if (!stub) sys.error("Cannot stop stub. Stub application not running.")
    App.prepareToShutdown()
    App.stop()
    stub = false
    app = null
  }

  /**
   * Запустить приложение-заглушку в режиме Mode.Test
   */
  def startTest = {
    App.maybeApp.filter(app == null || _ != app.get.right.get).foreach(_ => sys.error("Cannot create stub. Another application is running."))
    stub = true
    app = new TestAppProvider(new DefaultApplication(path, this.getClass.getClassLoader, Profile.Test))
    app
  }

  /**
   * Убедиться, что приложение-заглушка запущена
   */
  def ensure(profile: Profile, allowPlugins: Boolean = true) = app match {
    case null => start(profile, allowPlugins)
    case a =>
      require(app.profile == profile, "App started with another profile: " + app.profile + ", trying to start with profile: " + profile)
      a
  }

  // TODO: В консоли работает только один раз. После второго раза начинаются ошибки
  def withApp[R](profile: Profile, allowPlugins: Boolean = true)(block: => R): R = {
    ensure(profile, allowPlugins)
    try {
      block
    } finally {
      stop()
    }
  }

  def withAppDev[R](block: => R): R = withApp(Profile.Dev)(block)
  def withAppTest[R](block: => R): R = withApp(Profile.Test)(block)
  def withAppNoPluginsDev[R](block: => R): R = withApp(Profile.Dev, allowPlugins = false)(block)
  def withAppNoPluginsTest[R](block: => R): R = withApp(Profile.Test, allowPlugins = false)(block)

  /** Приложение действительно запущено в Production, и это не AppStub? */
  def isRealProd(app: Application): Boolean = !isStub && app.profile.isProd
}

