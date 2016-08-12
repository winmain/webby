package webby.core.server

import java.io.File
import java.{util => ju}

import webby.core.SBTLink

/**
  * Сервер для локальной разработки. Приспособлен останавливаться по нажатию на Enter в консоли.
  * Также, его можно немного изменить, переопределив некоторые методы внутри него.
  */
class DevServer {
  var firstReload = true

  def defaultPort = 9000

  // Start server
  val port: Int = Option(System.getProperty("http.port")).fold(defaultPort)(Integer.parseInt)
  val server = NettyServer.mainDev(port, Some(new SBTLink {
    override def runTask(name: String): AnyRef = ???
    override def reload(): AnyRef = if (firstReload) {
      firstReload = false
      getClass.getClassLoader
    } else null
    override def projectPath(): File = new File(".")
    override def settings(): ju.Map[String, String] = ju.Collections.emptyMap()
    override def forceReload(): Unit = ???
    override def findSource(className: String, line: Integer): Array[AnyRef] = {
      // Если выполнение кода заходит сюда, JRebel уже накосячил с заменой классов, и проекту
      // требуется перезагрузка
      System.exit(0)
      Array()
    }
  }))

  def onServerStarted(): Unit = {}
  onServerStarted()

  var shutdownHook: Thread = new Thread {
    override def run() {
      shutdownHook = null
      stopServers()
    }
  }
  Runtime.getRuntime.addShutdownHook(shutdownHook)

  // Run application
  server.applicationProvider.get.left.foreach {e =>
    stopServers()
    throw e
  }

  def slurpInStream() = while (System.in.available() > 0) System.in.read() // Прочитать все байты, которые были до этого в буфере
  slurpInStream()
  System.in.read() match {
    case -1 => // Сервер запущен без подключения терминала ввода, поэтому ничего не делаем
    case v =>
      slurpInStream()
      stopServers() // Получен enter с клавиатуры, завершаемся
  }

  def stopServers(): Unit = {
    if (shutdownHook != null) Runtime.getRuntime.removeShutdownHook(shutdownHook)
    server.stop()
  }
}

object DevServer {
  def main(args: Array[String]): Unit = {
    new DevServer
  }
}
