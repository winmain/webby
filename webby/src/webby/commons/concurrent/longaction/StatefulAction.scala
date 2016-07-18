package webby.commons.concurrent.longaction

import java.io.File

import webby.api.mvc.{PlainResult, Results}
import webby.commons.io.StdJs
import webby.mvc.StdPaths

import scala.reflect.ClassTag

/**
  * LongAction, имеющий внутреннее состояние, которое можно сериализовать в json.
  * Само действие может останавливаться по запросу и продолжать работу после рестарта сервера.
  *
  * Важно! Каждое такое действие должно быть перечислено в [[BaseStatefulActionsPlugin.controllerClasses]]
  */
abstract class StatefulAction[S <: StatefulState : ClassTag] extends LongAction[S] {
  def classTag = implicitly[ClassTag[S]]
  override def cancelable: Boolean = true
  override def threadName: String = "StatefulAction: " + name
  override final def stateful: Boolean = true

  var state: S = _

  override def status: LongActionStatus = state.las

  def start(state: S): Int = {
    this.state = state
    LongActions.runStateful(this)
  }

  def startOk(state: S): PlainResult = Results.Ok(start(state).toString)

  /**
    * Полезный метод, который проверяет:
    * 1. Не завершается ли сервер сейчас. Если завершается, то он бросает [[InterruptedException]],
    * что является нормой для выхода из треда.
    * 2. Не желает ли юзер отменить задание?
    *
    * Автосохранение в этих двух случаях происходит автоматически
    *
    * @return true - если можно продолжать выполнение,
    *         false - если следует завершить задание досрочно.
    */
  protected def checkRunning: Boolean = {
    if (!LongActions.running) {
      saveState()
      throw new InterruptedException("LongActions not running")
    } else if (status.isCancelRequested) {
      clearState()
      status.doCancel()
      false
    } else {
      true
    }
  }

  protected def iterate[T](it: Iterator[T], autoSaveEvery: Int = 0)(fn: T => Any) {
    var i = 0
    it.foreach {item =>
      if (!checkRunning) return

      fn(item)

      if (autoSaveEvery > 0) {
        i += 1
        if (i == autoSaveEvery) {
          saveState() // Сохранение состояния на случай внезапной остановки сервера.
          i = 0
        }
      }
    }
  }

  def stateFile = new File(StdPaths.get.state.toFile, "StatefulAction-" + name + ".json")

  def saveState() {
    val file: File = stateFile
    file.getParentFile.mkdirs()
    StdJs.get.mapper.writeValue(file, state)
  }
  def clearState() {
    val file: File = stateFile
    if (file.exists()) file.delete()
  }
  def loadState(): Option[S] = {
    val file: File = stateFile
    if (file.exists()) Some(StdJs.get.mapper.readValue(file, classTag.runtimeClass.asInstanceOf[Class[S]]))
    else None
  }

  def loadStateAndStart(): Unit = {
    loadState().foreach(start)
  }

  private[longaction] def launch0(): Unit = {
    saveState()
    launch()
    status.finish()
    clearState()
  }

  // ------------------------------- Abstract methods -------------------------------

  protected def launch(): Unit
}
