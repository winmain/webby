package webby.commons.concurrent.longaction
import org.slf4j.LoggerFactory
import webby.api.mvc.{PlainResult, Results}
import webby.commons.system.cron.CronLogFactory

case class SimpleLongAction[R](name: String)
                              (val action: LongActionStatus => R) extends LongAction[R] {
  override final def stateful: Boolean = false

  override def cancelable: Boolean = status.cancelable

  override val status = new LongActionStatus()

  private var finishBlock: R => Unit = (_) => ()
  private var errorBlock: Exception => Unit = (exception) => status.onError(exception.toString)

  private var result: R = _

  override def threadName: String = "LongAction: " + name

  def withOnFinish(block: R => Unit): this.type = {finishBlock = block; this}
  def withOnError(block: Exception => Unit): this.type = {errorBlock = block; this}

  private[longaction] def getRunnable = new Runnable {
    def run() {
      val cronLog = CronLogFactory.get.forLongAction(name).start()
      try {
        result = action(status)
        status.finish()
        finishBlock(result)
      } catch {
        case e: Exception =>
          LoggerFactory.getLogger(getClass).warn("Error executing longAction " + name, e)
          errorBlock(e)
      } finally {
        cronLog.finish()
      }
    }
  }

  def start: Int = LongActions.runSimple(this)

  def startOk: PlainResult = Results.Ok(start.toString)
}
