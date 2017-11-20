package webby.commons.concurrent.longaction

import javax.annotation.Nullable

import querio.{CountedLazyResult, ScalaDbEnum, ScalaDbEnumCls}
import webby.commons.io.jackson.JacksonAnnotations._

import scala.util.control.Breaks

class LongActionStatus(@JsonProperty var progress: Int = 0,
                       @JsonProperty var maxProgress: Int = 0,
                       @JsonProperty var message: String = "",
                       @JsonProperty var error: String = null,
                       @JsonProperty var timeTook: Long = 0) {
  @JsonProperty private var _state = LongActionState.ready
  def state: LongActionState = _state

  @JsonProperty private var _startTime = 0L
  def startTime: Long = _startTime

  /**
   * Cancelable определяется автоматически. Если action вызывает метод [[isCancelRequested]],
   * значит он поддерживает отмену.
   */
  private var _cancelable: Boolean = false
  def cancelable: Boolean = _cancelable

  /**
   * Если состояние берёт свои значения из другого места динамически, то эта функция обеспечивает
   * обновление значений этого состояния по колбеку. Оно вызывается каждый раз при выводе статуса
   * этого состояния.
   */
  @Nullable var updater: LongActionStatus => Any = _

  /**
   * Обновить состояние, вызвав updater, и вернуть себя. Применяется для тех состояний, которые
   * обновляют данные только по колбеку.
   */
  def updated: this.type = { if (updater != null) updater(this); this }

  def percentProgress: Option[Int] = if (maxProgress == 0) None else Some((progress * 100) / maxProgress)

  def isCancelRequested = {
    _cancelable = true
    _state == LongActionState.cancelRequested
  }

  def start(statefulAction:Boolean) {
    _state match {
      case LongActionState.ready =>
        _state = LongActionState.started
        message = "Started"
        _startTime = System.currentTimeMillis()

      case LongActionState.started if statefulAction =>
        message = "Resume"

      case s => sys.error("Cannot start action while in state " + s)
    }
  }

  def finish() {
    def saveTime(): Unit = {
      timeTook = System.currentTimeMillis() - _startTime
    }

    _state match {
      case LongActionState.cancelRequested =>
        _state = LongActionState.cancelled
        saveTime()

      case LongActionState.started =>
        _state = LongActionState.finished
        progress = maxProgress
        saveTime()

      case LongActionState.cancelled =>
        saveTime()

      case s => sys.error("Cannot finish action while in state " + s)
    }
  }

  def requestCancel() {
    require(_state == LongActionState.started, "Cannot requestCancel action while in state " + _state)
    _state = LongActionState.cancelRequested
  }

  def doCancel() {
    require(_state == LongActionState.cancelRequested, "Cannot cancel action while in state " + _state)
    _state = LongActionState.cancelled
  }

  def onError(error: String) {
    this.error = error
    _state = LongActionState.error
  }

  private[longaction] def setKilledStatus(): Unit = {
    _state = LongActionState.killed
  }

  // ------------------------------- Utility method -------------------------------

  // Cancellable wrapped iterator
  def wrap[R](result: CountedLazyResult[R], message: String)(body: Iterator[R] => Any): Unit = {
    progress = 0
    maxProgress = result.count
    this.message = message
    Breaks.breakable {
      body(result.rows.map { item =>
        if (isCancelRequested || state == LongActionState.error) {
          doCancel()
          Breaks.break()
        }
        progress += 1
        item
      })
    }
  }

  // Cancellable foreach
  def foreach[R](values: Seq[R], message: String)(fn: R => Any): Unit = {
    progress = 0
    maxProgress = values.size
    this.message = message
    for (value <- values) {
      if (isCancelRequested || state == LongActionState.error) {
        doCancel()
        return
      }
      progress += 1
      fn(value)
    }
  }
}

/**
 * Описывает состояние действия.
 * Класс реализован в виде ScalaDbEnum вместо Enumeration из-за проблем с ClassLoader'ом,
 * который пытается загрузить этот файл при десериализации из Js.mapper.
 */
object LongActionState extends ScalaDbEnum[LongActionState] {
  def V = LongActionState
  val ready = V("ready")
  val started = V("started")
  val finished = V("finished")
  val cancelRequested = V("cancelRequested")
  val cancelled = V("cancelled")
  val error = V("error")
  val killed = V("killed")
}

case class LongActionState private(dbValue: String) extends ScalaDbEnumCls[LongActionState](LongActionState : ScalaDbEnum[LongActionState] /** idea workaround */, dbValue) {
  override def toString: String = dbValue
}
