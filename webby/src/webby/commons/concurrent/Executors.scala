package webby.commons.concurrent
import java.util.concurrent._

import webby.commons.concurrent.executors.{ExecutorScalingQueue, ForceQueuePolicy, ScalingThreadPoolExecutor}

object Executors {

  class Executor(service: ExecutorService) {
    def execute(command: => Any) {
      service.execute(new Runnable {
        override def run(): Unit = command
      })
    }
  }

  /**
    * Простая обёртка над ThreadPoolExecutor, используя SynchronousQueue и ForceQueuePolicy.
    * По сути, эта обёртка позволяет легко сделать параллельность выполнения заданий.
    * <p>
    * Пример:
    * {{{
    * Executors.synchronousQueueExecutor() { ex =>
    *    for (action <- actions) {
    *      ex.execute(action)
    *    }
    * }
    * }}}
    */
  def synchronousQueueExecutor[R](threads: Int = 4)(block: Executor => R): R = {
    val executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
      new SynchronousQueue[Runnable], new ForceQueuePolicy)

    try block(new Executor(executor))
    finally {
      executor.shutdown()
      executor.awaitTermination(1, TimeUnit.HOURS)
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
