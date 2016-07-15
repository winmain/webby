package webby.commons.concurrent

import java.util.concurrent._

import webby.api.Logger

import scala.collection.mutable

/**
  * Класс для выполнения заданий в заданном промежутке времени.
  * Если задания не успевают выполниться за отведённое время, то будет вызван System.exit(-1)
  */
class TimeLimitExecutor(log: Logger) {

  val pool: ExecutorService = createPool
  val tasks = mutable.Buffer[(Future[_], String)]()

  protected def createPool: ExecutorService = java.util.concurrent.Executors.newCachedThreadPool()

  def submit(runnable: Runnable, name: String): Future[_] = {
    val future = pool.submit(runnable)
    tasks += ((future, name))
    future
  }

  def submit(task: => Unit, name: String): Future[_] = submit(new Runnable {
    override def run(): Unit = task
  }, name)

  def waitForFinish(timeout: Long, unit: TimeUnit, stopOnTimeout: => Any) {
    pool.shutdown()
    val stillRunning = !pool.awaitTermination(timeout, unit)
    if (stillRunning) {
      log.error("Cannot stop tasks after time limit: " + tasks.withFilter(!_._1.isDone).map(_._2).mkString(", "))
      stopOnTimeout
    }
  }
}
