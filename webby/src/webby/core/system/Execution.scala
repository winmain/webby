package webby.core

import java.util.concurrent.Executors

import io.netty.util.concurrent.DefaultThreadFactory
import webby.api.App

import scala.concurrent.ExecutionContext

private[webby] object Execution {

  lazy val internalContext: scala.concurrent.ExecutionContextExecutorService = {
    val numberOfThreads = App.maybeApp.map(_.configuration.getInt("internal-threadpool-size")).flatten.getOrElse(Runtime.getRuntime.availableProcessors)

    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numberOfThreads, new DefaultThreadFactory("webby-internal-execution-context")))
  }
}
