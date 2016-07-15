package webby.commons.concurrent

import java.util.concurrent.TimeUnit

import io.netty.util.concurrent.DefaultThreadFactory
import webby.api.{App, Application, Plugin}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Простой плагин, содержащий в себе [[ExecutionContextExecutor]], который можно использовать
  * для фонового выполнения заданий.
  * Плагин управляет этим контекстом и корректно завершает его при остановке приложения.
  *
  * Типичное применение:
  * {{{
  * class Foo {
  *   private implicit lazy val executionContext: ExecutionContext = ExecutionPlugin.getExecutionContext
  *
  *   Future {
  *     // some action
  *   }
  * }
  * }}}
  *
  * @param maxThreads Максимальное количество тредов в контексте
  */
class ExecutionPlugin(app: Application, maxThreads: Int = 100) extends Plugin {
  private lazy val _executionContext: scala.concurrent.ExecutionContextExecutorService = {
    ExecutionContext.fromExecutorService(
      Executors.scalingExecutor("ExecutionPlugin", 0, maxThreads, 5L, TimeUnit.SECONDS,
        new DefaultThreadFactory("ExecutionPlugin")))
  }

  def executionContext: ExecutionContextExecutor = _executionContext

  override def onStop(): Unit = {
    _executionContext.shutdown()
    _executionContext.awaitTermination(15L, TimeUnit.SECONDS)
  }
}

object ExecutionPlugin {
  def getExecutionContext: ExecutionContext = App.app.plugin(classOf[ExecutionPlugin]).get.executionContext
}
