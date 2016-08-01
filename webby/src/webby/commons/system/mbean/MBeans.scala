package webby.commons.system.mbean
import webby.api.{App, Application, Logger, Profile}
import webby.commons.system.mbean.MBeanRegistrar.{DummyMBeanRegistrar, MBeanRegBuilder}
import webby.mvc.AppStub

/**
  * Глобальный объект, регистрирующий все MBean'ы
  *
  * Тесты должны вызывать метод #switchToDummy, чтобы заменить реальный регистратор на заглушку.
  */
object MBeans {

  private var baseDomain: String = "webby"
  private var lastApp: Application = null
  private var lastRegistrar: MBeanRegistrar = null

  App.addOnStopHandler {
    val reg = registrar
    if (reg != null) reg.unregisterAll()
  }

  def setBaseDomain(v: String): Unit = {
    if (isRealApp && lastRegistrar != null) Logger(getClass).warn("Setting baseDomain to " + v + " too late. Registrar already created. You should set baseDomain before calling any MBeans.register() method.")
    baseDomain = v
  }
  def getBaseDomain: String = baseDomain

  private def registrar: MBeanRegistrar = {
    if (!isRealApp) new DummyMBeanRegistrar
    else if (App.appOrNull == lastApp) lastRegistrar
    else {
      lastApp = App.appOrNull
      lastRegistrar = App.maybeApp match {
        case Some(app) =>
          app.profile match {
            case Profile.Prod | Profile.Jenkins => new CommonMBeanRegistrar(baseDomain)
            case Profile.Dev => new CommonMBeanRegistrar(baseDomain + "-" + System.currentTimeMillis())
            case _ => new DummyMBeanRegistrar
          }
        case _ => new DummyMBeanRegistrar
      }
      lastRegistrar
    }
  }

  def isRealApp: Boolean = !AppStub.isStub && App.maybeApp.fold(false)(!_.profile.isTest)

  def register[T](impl: T, mbeanInterface: Class[T]): MBeanRegBuilder = registrar.register(impl, mbeanInterface)
  def register(obj: AnyRef): MBeanRegBuilder = registrar.register(obj)

  def switchToDummy() {
    lastApp = App.appOrNull
    lastRegistrar = new DummyMBeanRegistrar
  }
}
