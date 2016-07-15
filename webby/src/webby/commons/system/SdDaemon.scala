package webby.commons.system

import java.io.OutputStream
import java.nio.file.{Files, LinkOption, Paths}

import com.etsy.net.{JUDS, UnixDomainSocketClient}
import org.slf4j.LoggerFactory

/**
  * Notifies systemd after server start.
  *
  * Required sbt dependency
  * {{{
  *   deps += "uk.co.caprica" % "juds" % "0.94.1"
  * }}}
  */
object SdDaemon {
  val log = LoggerFactory.getLogger(getClass)

  def ready(): Unit = {
    if (!Files.isDirectory(Paths.get("/run/systemd/system"), LinkOption.NOFOLLOW_LINKS)) {
      log.error("Cannot notify - not booted")
      return
    }

    val notifySocket = System.getenv("NOTIFY_SOCKET")
    if (notifySocket == null) {
      log.error("Cannot notify - NOTIFY_SOCKET not set")
      return
    }

    val client: UnixDomainSocketClient = new UnixDomainSocketClient(notifySocket, JUDS.SOCK_DGRAM)
    val os: OutputStream = client.getOutputStream
    os.write("READY=1".getBytes())
    os.flush()
    client.close()
  }
}
