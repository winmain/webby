package webby.commons.concurrent.longaction
import webby.commons.text.SB
import webby.commons.text.StringWrapper.wrapper

trait LongAction[R] {
  def name: String
  def cancelable: Boolean
  def stateful: Boolean

  def status: LongActionStatus

  def threadName: String

  /**
    * Строка для MBean
    */
  def toMBeanString: String = {
    val status = this.status.updated
    status.state match {
      case LongActionState.finished | LongActionState.cancelled => name + ": " + status.state.toString
      case _ => new SB {
        +name + ": " + status.state.toString
        status.percentProgress.foreach(percent =>
          +", " + status.progress + " of " + status.maxProgress + " (" + percent + "%)"
        )
        if (status.message.isNotEmpty) {
          +", " + status.message
        }
      }.str
    }
  }
}
