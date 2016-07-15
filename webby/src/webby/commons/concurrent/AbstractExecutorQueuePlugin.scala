package webby.commons.concurrent

import java.util.concurrent.{BlockingQueue, LinkedBlockingDeque, ThreadPoolExecutor, TimeUnit}

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import webby.api.Plugin
import webby.commons.bean.annotation.Description
import webby.commons.collection.LazyIterator

/**
  * Плагин, который содержит в себе очередь и executor для выполнения действий над
  * записями в этой очереди.
  */
abstract class AbstractExecutorQueuePlugin extends Plugin with AbstractExecutorQueuePluginMBean {
  /** Тип элементов в очереди */
  type QueueItem

  protected val log = webby.api.Logger(getClass)

  /** Название группы тредов, создаваемых внутренним executor'ом */
  protected def executorThreadGroupName: String
  /** Стартовое и минимальное количество потоков executor'а */
  protected def executorCorePoolSize: Int = 1
  /** Макс. количество потоков executor'а */
  protected def executorMaxPoolSize: Int = 4

  /** Макс. размер очереди */
  def maxQueueSize = 10000

  /** Мин. количество оставшегося места в очереди (когда очередь приближается к максимальному размеру),
    * прежде чем начать паниковать ворнингами в лог.
    */
  protected def minQueueRemainingCapacity = 10

  /**
    * Очередь записей
    */
  protected val queue: BlockingQueue[QueueItem] = new LinkedBlockingDeque[QueueItem](maxQueueSize)

  /**
    * Executor тредов-обработчиков записей
    */
  protected var executor: ThreadPoolExecutor = null

  /**
    * Проверить запись на корректность прежде чем добавлять её в очередь.
    * Если запись некорректна, то здесь следует бросить Exception.
    */
  protected def checkItem(item: QueueItem): Unit = {}

  /**
    * Добавить запись в очередь на обработку
    */
  def submit(item: QueueItem): Unit = {
    checkItem(item)
    if (queue.remainingCapacity() < minQueueRemainingCapacity) {
      // Внештатная ситуация. В этом случае нужно выяснить причину переполнения очереди
      log.warn("Remaining capacity is less than " + minQueueRemainingCapacity + "!")
    }
    queue.put(item)
    if (!executor.isShutdown && executor.getQueue.remainingCapacity() > 0) {
      executor.execute(new Runnable {
        override def run(): Unit = {
          while (!executor.isShutdown) {
            queue.poll() match {
              case null => return
              case mail =>
                try process(mail)
                catch {case e: Exception => log.error("Error processing item", e)}
            }
          }
        }
      })
    }
  }

  // ------------------------------- Abstract methods -------------------------------

  /**
    * Обработать запись из очереди
    */
  protected def process(item: QueueItem)

  /**
    * Восстановить очередь из файла после рестарта сервера.
    * После восстановления файл следует удалить.
    */
  protected def restoreQueue()

  /**
    * Сохранить очередь в файл перед рестартом сервера.
    * Если очередь пустая, то файла быть не должно.
    */
  protected def saveQueue(toSave: Option[Iterator[QueueItem]])

  /**
    * Вывести всю очередь в виде массива для MBean
    */
  def allQueueItems(): Array[String]

  // ------------------------------- Plugin methods -------------------------------

  /**
    * Called when the application starts.
    */
  override def onStart(): Unit = {
    executor = webby.commons.concurrent.Executors.scalingExecutor(executorThreadGroupName,
      executorCorePoolSize, executorMaxPoolSize, 1L, TimeUnit.SECONDS,
      new BasicThreadFactory.Builder().namingPattern(executorThreadGroupName + "-%d").build())
    restoreQueue()
  }

  /**
    * Called before application shutdown.
    */
  override def onPrepareToShutdown(): Unit = {
    if (executor != null) {
      executor.shutdown()
      saveAllQueue()
      executor.awaitTermination(1, TimeUnit.MINUTES)
    }
  }

  /**
    * Called when the application stops.
    */
  override def onStop(): Unit = {
    if (queue.size() != 0) log.warn("Queue not empty, still has " + queue.size() + " elements")
  }

  private def saveAllQueue(): Unit = {
    if (queue.isEmpty) saveQueue(None)
    else saveQueue(Some(new LazyIterator[QueueItem](Option(queue.poll()))))
  }

  // ------------------------------- MBean methods -------------------------------

  override def getActiveCount: Int = executor.getActiveCount
  override def getMaxPoolSize: Int = executorMaxPoolSize
  override def getQueueSize: Int = queue.size()
  override def getMaxQueueSize: Int = maxQueueSize
  override def clearQueue(): Unit = queue.clear()
}

trait AbstractExecutorQueuePluginMBean {
  @Description("Приблизительное количество тредов, активно обрабатывающих записи")
  def getActiveCount: Int

  @Description("Максимальное количество рабочих тредов")
  def getMaxPoolSize: Int

  @Description("Размер очереди")
  def getQueueSize: Int

  @Description("Максимальный размер очереди")
  def getMaxQueueSize: Int

  @Description("Очередь записей, которые предстоит обработать")
  def allQueueItems(): Array[String]

  @Description("Полностью очистить всю очередь")
  def clearQueue()
}
