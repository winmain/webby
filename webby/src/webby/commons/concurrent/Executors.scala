package webby.commons.concurrent
import java.util.concurrent._

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.slf4j.LoggerFactory
import webby.commons.concurrent.executors.{ExecutorScalingQueue, ForceQueuePolicy, ScalingThreadPoolExecutor}

object Executors {

  class Executor(val service: ExecutorService) {
    def execute[A](command: => A): java.util.concurrent.Future[A] = {
      service.submit(new Callable[A] {
        override def call(): A =
          try command
          catch {
            case e: Throwable =>
              LoggerFactory.getLogger(Executors.getClass).error("Unhandled exception", e)
              throw e
          }
      })
    }
  }

  /**
    * Простая обёртка над ThreadPoolExecutor, используя SynchronousQueue и ForceQueuePolicy.
    * По сути, эта обёртка позволяет легко сделать параллельность выполнения заданий.
    * <p>
    * Пример:
    * {{{
    * Executors.synchronousQueueExecutor("actions-%d", 4) { ex =>
    *    for (action <- actions) {
    *      ex.execute(action)
    *    }
    * }
    * }}}
    *
    * @param threads             Fixed thread number
    * @param threadNamingPattern Thread naming pattern, like "JobAppSender-%d" (see [[org.apache.commons.lang3.concurrent.BasicThreadFactory]])
    * @param awaitTerminationFor Time to wait for executor after shutdown
    */
  def withSynchronousQueueExecutor[R](threadNamingPattern: String,
                                      threads: Int,
                                      awaitTerminationFor: (Long, TimeUnit) = (1, TimeUnit.HOURS)
                                     )(block: Executor => R): R = {
    val executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
      new SynchronousQueue[Runnable],
      new BasicThreadFactory.Builder().namingPattern(threadNamingPattern).build(),
      new ForceQueuePolicy)

    try block(new Executor(executor))
    finally {
      executor.shutdown()
      executor.awaitTermination(awaitTerminationFor._1, awaitTerminationFor._2)
    }
  }

  /**
    * Создать масштабируемый ThreadPoolExecutor.
    * @see https://github.com/kimchy/kimchy.github.com/blob/master/_posts/2008-11-23-juc-executorservice-gotcha.textile
    * @param name          Имя пула
    * @param min           Минимальное количество потоков
    * @param max           Максимальное количество потоков
    * @param keepAliveTime Таймаут удаления неиспользуемых потоков
    * @param unit          Таймаут удаления неиспользуемых потоков
    * @param threadFactory Фабрика создания тредов для пула
    */
  def scalingExecutor(name: String, min: Int, max: Int, keepAliveTime: Long, unit: TimeUnit,
                      threadFactory: ThreadFactory = java.util.concurrent.Executors.defaultThreadFactory()): ScalingThreadPoolExecutor = {
    val queue = new ExecutorScalingQueue[Runnable]
    val executor = new ScalingThreadPoolExecutor(name, min, max, keepAliveTime, unit, queue, threadFactory, new ForceQueuePolicy)
    queue.executor = executor
    executor
  }
}
