package orm.elasticsearch

import java.net.InetAddress
import java.util
import java.util.concurrent.RejectedExecutionException

import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import webby.api.{Application, Plugin}
import webby.mvc.AppStub

class ElasticPlugin(app: Application) extends Plugin {

  private var _client: Client = _

  val clusterName = app.configuration.getString("elastic.clusterName").getOrElse(sys.error("No elastic.clusterName defined"))

  val transportAddresses: Vector[InetSocketTransportAddress] = {
    val result = Vector.newBuilder[InetSocketTransportAddress]
    app.configuration.getStringList("elastic.transportAddress")
      .getOrElse(util.Arrays.asList("127.0.0.1:9300"))
      .forEach {line =>
        val idx = line.lastIndexOf(':')
        val host = line.substring(0, idx)
        val port = line.substring(idx + 1).toInt
        result += new InetSocketTransportAddress(InetAddress.getByName(host), port)
      }
    result.result()
  }

  val httpPort = app.configuration.getInt("elastic.httpPort").getOrElse(9200)

  def client: Client = {
    if (_client == null) synchronized {
      if (_client == null) _client = startClient()
    }
    _client
  }

  override def onStart() {
    if (AppStub.isRealProd(app)) _client = startClient()
  }

  override def onStop() {
    if (_client != null) {
      try {
        _client.close()
      } catch {
        case e: RejectedExecutionException => webby.api.Logger(getClass).info("ElasticSearch bug on shutdown: " + e.toString)
      }
    }
  }

  protected def startClient(): Client = {
    // Instruct Elastic not to set netty available processors, because we already set it in webby
    System.setProperty("es.set.netty.runtime.available.processors", "false")

    val client = new PreBuiltTransportClient(Settings.builder().put("cluster.name", clusterName).build())
    transportAddresses.foreach {ta =>
      client.addTransportAddress(ta)
    }
    client
  }
}
