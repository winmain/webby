package webby.commons.bean
import webby.api.{App, Application, Profile}
import webby.commons.bean.mbean.MBeanRegistrar._
import webby.commons.bean.mbean.{CommonMBeanRegistrar, MBeanRegistrar}
import webby.mvc.AppStub

/**
  * Глобальный объект, регистрирующий все MBean'ы
  *
  * Тесты должны вызывать метод #switchToDummy, чтобы заменить реальный регистратор на заглушку.
  */
object MBeans {

  private var lastApp: Application = null
  private var lastRegistrar: MBeanRegistrar = null

  App.addOnStopHandler {
    val reg = registrar
    if (reg != null) reg.unregisterAll()
  }

  private def registrar: MBeanRegistrar = {
    if (!isRealApp) new DummyMBeanRegistrar
    else if (App.appOrNull == lastApp) lastRegistrar
    else {
      lastApp = App.appOrNull
      lastRegistrar = App.maybeApp match {
        case Some(app) =>
          app.profile match {
            case Profile.Prod | Profile.Jenkins => new CommonMBeanRegistrar("rosrabota")
            case Profile.Dev => new CommonMBeanRegistrar("rosrabota-" + System.currentTimeMillis())
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
