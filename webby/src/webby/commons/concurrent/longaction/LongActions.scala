package webby.commons.concurrent.longaction
import java.util

import webby.api.{App, HostProfileFacade}
import webby.commons.concurrent.ThreadUtils
import webby.commons.system.cron.CronLogFactory
import webby.commons.time.StdDates

import scala.collection.immutable.IntMap
import scala.reflect.ClassTag
import scala.util.Random

object LongActions extends LongActionsMBean {
  val log = webby.api.Logger(getClass)

  private val simpleActionThreads = new ThreadGroup("LongActions")
  private val statefulActionThreads = new ThreadGroup("StatefulActions")
  private var active = IntMap[(Thread, LongAction[_])]()
  private var finished = IntMap[(Long, LongAction[_])]()

  val FINISHED_ACTION_LIFETIME = 5 * StdDates.Minute

  private var _running = true
  def running = _running

  def runSimple(action: SimpleLongAction[_]): Int = {
    if (!running) throw new IllegalStateException("Cannot run simple action while not running")
    runAction(action, simpleActionThreads, action.getRunnable)
  }

  def runStateful(action: StatefulAction[_]): Int = {
    if (!running) throw new IllegalStateException("Cannot run stateful action while not running")
    val ctlFactories = App.app.plugin(classOf[BaseStatefulActionsPlugin]).getOrElse(sys.error("No StatefulActionsPlugin")).controllerClasses
    if (!ctlFactories.contains(action.getClass)) sys.error(action.getClass + " not registered in StatefulActionsPlugin.ctlFactories")

    getStatefulAction(action.name) match {
      case Some((id, _)) =>
        // У нас уже крутится такой action. Новый мы стартовать не будем, отдадим идешку от текущего
        id
      case None =>
        // Запустим новый action
        runAction(action, statefulActionThreads, new Runnable {
          override def run(): Unit = {
            val cronLog = CronLogFactory.get.forLongAction(action.name).start()
            try {
              action.launch0()
            } catch {
              case e: InterruptedException => () // ok, just exit
              case e: Exception =>
                action.status.onError(e.toString)
                log.error("Error executing action " + action.name, e)
            } finally {
              cronLog.finish()
            }
          }
        })
    }
  }

  def getStatefulAction[A <: StatefulAction[_]](implicit ct: ClassTag[A]): Option[(Int, A)] =
    active.collectFirst {case (id, (thread, a)) if a.stateful && ct.runtimeClass.isInstance(a) => (id, a.asInstanceOf[A])}
  def getStatefulAction(name: String): Option[(Int, StatefulAction[_])] =
    active.collectFirst {case (id, (thread, action: StatefulAction[_])) if action.stateful && action.name == name => (id, action)}

  def getAction(id: Int): Option[LongAction[_]] = active.get(id).orElse(finished.get(id)).map(_._2)

  def getActiveActions: IntMap[(Thread, LongAction[_])] = active
  def getFinishedActions: IntMap[(Long, LongAction[_])] = finished

  /**
    * Информация обо всех запущенных действиях (mbean)
    */
  override def getActiveActionsInfo: util.List[String] = {
    val res = new util.ArrayList[String]()
    for ((id, (_, action)) <- active) yield {
      res.add(id + ": " + action.toMBeanString)
    }
    res
  }

  /**
    * Информация о прошедших действиях (mbean)
    */
  override def getFinishedActionsInfo: util.List[String] = {
    val res = new util.ArrayList[String]()
    for ((id, (_, action)) <- finished) yield {
      res.add(id + ": " + action.toMBeanString)
    }
    res
  }

  /**
    * Послать запрос на отмену действия (mbean)
    */
  override def cancelAction(id: Int): String = {
    getAction(id) match {
      case None => "Action " + id + " not found"
      case Some(action) =>
        if (!action.cancelable) "Action not cancelable"
        else {
          val status: LongActionStatus = action.status
          status.state match {
            case LongActionState.cancelled => "Action already cancelled"
            case LongActionState.finished => "Action already finished"
            case LongActionState.cancelRequested => "Cancel already requested"
            case _ =>
              status.requestCancel()
              "ok"
          }
        }
    }
  }

  /**
    * Аварийная остановка действия (mbean)
    */
  @SuppressWarnings(Array("deprecation"))
  override def killAction(id: Int): String = {
    active.get(id) match {
      case None => "Action " + id + " not found or already stopped"
      case Some((thread, action)) =>
        thread.interrupt()
        var i = 0
        while (i < 10 && thread.isAlive) {
          Thread.sleep(100)
          i += 1
        }
        val msg: String =
          if (thread.isAlive) {
            ThreadUtils.stop(thread)
            "Action killed"
          } else {
            "Action safely interrupted"
          }
        action.status.setKilledStatus()
        finishAction(id, action)
        msg
    }
  }

  /**
    * Запустить все сохранённые StatefulActions
    */
  override def restoreAllStatefulActions(): Unit = App.app.plugin[BaseStatefulActionsPlugin].get.restoreAllControllers()


  // ------------------------------- Private & protected methods -------------------------------

  private def nextId(): Int = math.abs(Random.nextInt())

  private def runAction(action: LongAction[_], threadGroup: ThreadGroup, runnable: Runnable): Int = {
    val hostProfile = HostProfileFacade.get
    action.status.start(action.stateful)
    val id: Int = nextId()
    val thread = new Thread(threadGroup, action.threadName) {
      override def run() {
        if (App.isDevOrJenkins) HostProfileFacade.localSet(hostProfile)
        runnable.run()
        finishAction(id, action)
      }
    }
    active.synchronized {
      active = active.updated(id, (thread, action))
    }
    thread.start()
    id
  }

  private def finishAction(id: Int, action: LongAction[_]) {
    active.synchronized {
      active = active - id
    }
    finished.synchronized {
      finished = finished.updated(id, (System.currentTimeMillis() + FINISHED_ACTION_LIFETIME, action))
    }
    cleanFinishedActions
  }

  private def cleanFinishedActions = finished.synchronized {
    val time = System.currentTimeMillis()
    finished -- finished.filter(_._2._1 < time).keys
  }

  private[longaction] def stopAndWait() {
    _running = false
    locally {
      val threads = Array.ofDim[Thread](simpleActionThreads.activeCount())
      simpleActionThreads.enumerate(threads)
      threads.foreach(_.interrupt())
    }
    locally {
      val threads = Array.ofDim[Thread](statefulActionThreads.activeCount())
      statefulActionThreads.enumerate(threads)
      threads.foreach(_.join())
    }
  }
}
