package webby.commons.concurrent.longaction

import webby.api._

import scala.util.control.NonFatal

/**
  * Плагин управления контроллерами с состояниями.
  * При старте запускает контроллеры, у которых было сохранено предыдущее состояние.
  * При остановке, сохраняет состояние запущенных контроллеров.
  *
  * @param controllerClasses Классы всех контроллеров, наследников [[StatefulAction]], используемых
  *                          в проекте
  */
abstract class BaseStatefulActionsPlugin(val controllerClasses: Seq[Class[_ <: StatefulAction[_ <: StatefulState]]]) extends Plugin {

  // Для консоли мы не восстанавливаем сохранённые состояния.
  def active: Boolean = !App.isDevOrConsole

  override def onStart(): Unit = {
    if (active) restoreAllControllers()
  }

  override def onPrepareToShutdown(): Unit = {
    if (active) LongActions.stopAndWait()
  }

  /**
    * Запустить все сохранённые StatefulActions
    */
  def restoreAllControllers(): Unit =
  for (cls <- controllerClasses) {
    try {
      cls.newInstance().loadStateAndStart()
    } catch {
      case NonFatal(e) => Logger.warn("Cannot restore StatefulAction " + cls.getName, e)
    }
  }
}
