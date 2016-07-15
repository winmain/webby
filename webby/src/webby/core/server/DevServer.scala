package webby.core.server

import java.io.File
import java.{util => ju}

import webby.core.SBTLink

object DevServer {
  def main(args: Array[String]) {
    var firstReload = true

    // Start server
    val port: Int = Option(System.getProperty("http.port")).fold(9000)(Integer.parseInt)
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
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run() {
        server.stop()
      }
    })

    // Run application
    server.applicationProvider.get.left.foreach {e =>
      server.stop()
      throw e
    }

    def slurpInStream() = while (System.in.available() > 0) System.in.read() // Прочитать все байты, которые были до этого в буфере
    slurpInStream()
    System.in.read() match {
      case -1 => // Сервер запущен без подключения терминала ввода, поэтому ничего не делаем
      case v =>
        slurpInStream()
        server.stop() // Получен enter с клавиатуры, завершаемся
    }
  }
}
