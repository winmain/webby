package webby.commons.concurrent

import java.util.concurrent.{ExecutorService, Future, SynchronousQueue, ThreadFactory, ThreadPoolExecutor, TimeUnit, Callable => JavaCallable}

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.slf4j.LoggerFactory
import webby.commons.concurrent.executors.{ExecutorScalingQueue, ForceQueuePolicy, ScalingThreadPoolExecutor}

object Executors {

  /**
    * Simple wrapper for [[ExecutorService]] to support scala lambdas and logging unhandled exceptions
    * in threads
    */
  class Executor(val service: ExecutorService) {
    def execute[A](command: => A): Future[A] = {
      service.submit(new JavaCallable[A] {
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
    * Simple wrapper for [[Executor]].
    * This wrapper eases multitask programming.
    *
    * @param executor            The task executor
    * @param awaitTerminationFor How long to wait for executor tasks to finish after #block executed
    * @param block               A wrapped body where executor is used
    */
  def withExecutor[R](executor: Executor,
                      awaitTerminationFor: (Long, TimeUnit) = (1, TimeUnit.HOURS)
                     )(block: Executor => R): R = {
    try block(executor)
    finally {
      executor.service.shutdown()
      executor.service.awaitTermination(awaitTerminationFor._1, awaitTerminationFor._2)
    }
  }

  /**
    * Auto-closing wrapper for [[ExecutorService]].
    * This wrapper eases multitask programming.
    *
    * @param executor            The task executor
    * @param awaitTerminationFor How long to wait for executor tasks to finish after #block executed
    * @param block               A wrapped body where executor is used
    */
  def withExecutorService[R](executor: ExecutorService,
                             awaitTerminationFor: (Long, TimeUnit) = (1, TimeUnit.HOURS)
                            )(block: Executor => R): R =
    withExecutor(new Executor(executor), awaitTerminationFor)(block)

  /**
    * Simple wrapper for [[ThreadPoolExecutor]] using [[SynchronousQueue]] and [[ForceQueuePolicy]].
    * This wrapper eases multitask programming.
    * <p>
    * Example:
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
    * @param awaitTerminationFor How long to wait for executor tasks to finish after #block executed
    * @param block               A wrapped body where executor is used
    */
  def withSynchronousQueueExecutor[R](threadNamingPattern: String,
                                      threads: Int,
                                      awaitTerminationFor: (Long, TimeUnit) = (1, TimeUnit.HOURS)
                                     )(block: Executor => R): R = {
    val executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
      new SynchronousQueue[Runnable],
      new BasicThreadFactory.Builder().namingPattern(threadNamingPattern).build(),
      new ForceQueuePolicy)
    withExecutor(new Executor(executor), awaitTerminationFor)(block)
  }

  // ------------------------------- Scaling executors -------------------------------

  /**
    * Create a real scaling [[ThreadPoolExecutor]].
    * @see https://github.com/kimchy/kimchy.github.com/blob/master/_posts/2008-11-23-juc-executorservice-gotcha.textile
    * @param name          Pool name
    * @param min           Minimal thread count
    * @param max           Maximal thread count
    * @param keepAliveTime Timeout for unused threads
    * @param unit          Timeout for unused threads
    * @param threadFactory Thread creation factory. Please use a thread factory with a thread naming pattern.
    */
  def scalingExecutor(name: String, min: Int, max: Int, keepAliveTime: Long, unit: TimeUnit,
                      threadFactory: ThreadFactory = java.util.concurrent.Executors.defaultThreadFactory()): ScalingThreadPoolExecutor = {
    val queue = new ExecutorScalingQueue[Runnable]
    val executor = new ScalingThreadPoolExecutor(name, min, max, keepAliveTime, unit, queue, threadFactory, new ForceQueuePolicy)
    queue.executor = executor
    executor
  }

  /**
    * Create a real scaling [[ThreadPoolExecutor]].
    * @see https://github.com/kimchy/kimchy.github.com/blob/master/_posts/2008-11-23-juc-executorservice-gotcha.textile
    * @param min                 Minimal thread count
    * @param max                 Maximal thread count
    * @param keepAliveTime       Timeout for unused threads
    * @param unit                Timeout for unused threads
    * @param threadNamingPattern Thread naming pattern, like "JobAppSender-%d" (see [[org.apache.commons.lang3.concurrent.BasicThreadFactory]])
    */
  def scalingExecutor(min: Int, max: Int, keepAliveTime: Long, unit: TimeUnit, threadNamingPattern: String): ScalingThreadPoolExecutor = {
    val threadFactory = new BasicThreadFactory.Builder().namingPattern(threadNamingPattern).build()
    scalingExecutor(threadNamingPattern, min, max, keepAliveTime, unit, threadFactory)
  }

  /**
    * Simple wrapper for [[scalingExecutor()]].
    *
    * @param threadNamingPattern Thread naming pattern, like "JobAppSender-%d" (see [[org.apache.commons.lang3.concurrent.BasicThreadFactory]])
    * @param min                 Minimal thread count
    * @param max                 Maximal thread count
    * @param keepAliveTime       Timeout for unused threads
    * @param awaitTerminationFor How long to wait for executor tasks to finish after #block executed
    * @param block               A wrapped body where executor is used
    */
  def withScalingExecutor[R](threadNamingPattern: String,
                             min: Int,
                             max: Int,
                             keepAliveTime: (Long, TimeUnit) = (5, TimeUnit.SECONDS),
                             awaitTerminationFor: (Long, TimeUnit) = (1, TimeUnit.HOURS)
                            )(block: Executor => R): R = {
    withExecutor(new Executor(scalingExecutor(min, max, keepAliveTime._1, keepAliveTime._2, threadNamingPattern)))(block)
  }
}
