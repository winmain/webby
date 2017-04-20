package webby.commons.system.cron

import javax.annotation.Nullable

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.quartz.JobExecutionContext

import scala.reflect.ClassTag

/**
  * Контекст-абстракция для крон-заданий.
  */
trait CronJobContext {
  /** Оригинальный Quartz контекст выполнения задания. Может быть null, если задание не запущено Quartz'ом. */
  @Nullable def jec: JobExecutionContext

  /** Quartz перешёл в режим завершения? */
  def isShutdown: Boolean

  /** Quartz task, котороый выполняется в этом контексте хотябы раз опрашивал состояние флага [[isShutdown]]?
    * По этому параметру можно определить поддерживает ли таск быструю остановку сервера, или нет.
    */
  def isTaskShutdownChecked: Boolean

  /**
    * Получить сохранённое ранее состояние. Это состояние должно уметь сериализоваться в JSON.
    */
  def getInitState[T](implicit ct: ClassTag[T]): Option[T]

  /**
    * Сохранение состояния для его последующего восстановления после рестарта сервера.
    * Состояние должно уметь сериализоваться в JSON.
    *
    * @param state Состояние для сохранения. Если null, то ничего не сохраняем, и не вызываем это
    *              крон-задание после рестарта сервера.
    */
  def setFinishState(@Nullable state: AnyRef)
}

/**
  * Абстракция для quartz cron
  */
class QuartzCronJobContext(val jec: JobExecutionContext, initStateNode: Option[JsonNode], jsMapper: ObjectMapper) extends CronJobContext {
  private var taskShutdownChecked = false
  var finishState: AnyRef = null

  override def isShutdown: Boolean = {taskShutdownChecked = true; jec.getScheduler.isInStandbyMode}
  override def isTaskShutdownChecked: Boolean = taskShutdownChecked

  override def getInitState[T](implicit ct: ClassTag[T]): Option[T] = {
    initStateNode.map {node =>
      jsMapper.treeToValue(node, ct.runtimeClass.asInstanceOf[Class[T]])
    }
  }

  override def setFinishState(state: AnyRef): Unit = finishState = state
}

/**
  * Абстракция для запусков заданий из командной строки
  */
object CronJobContextStub extends CronJobContext {
  override def jec: JobExecutionContext = null
  override def isShutdown: Boolean = false
  override def isTaskShutdownChecked: Boolean = false
  override def getInitState[T](implicit ct: ClassTag[T]): Option[T] = None
  override def setFinishState(state: AnyRef): Unit = sys.error("Inapplicable for CronContextStub")
}
