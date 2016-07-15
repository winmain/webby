package webby.api.libs.concurrent


object Execution {

  object Implicits {
    implicit lazy val defaultContext: scala.concurrent.ExecutionContext =
      webby.core.Invoker.executionContext: scala.concurrent.ExecutionContext
  }

  val defaultContext = Implicits.defaultContext

}

