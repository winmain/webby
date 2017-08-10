package orm.elasticsearch

import java.io.{FileNotFoundException, IOException}
import java.net.{HttpURLConnection, URL}

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.netty.handler.codec.http.HttpMethod
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.{ActionRequest, ActionRequestBuilder, ActionResponse}
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.NoNodeAvailableException
import org.elasticsearch.common.unit.TimeValue
import org.slf4j.LoggerFactory
import querio.TableRecord
import webby.commons.io.IOUtils
import webby.commons.io.jackson.JacksonAnnotations._
import webby.commons.system.log.PageLog
import webby.mvc.AppPluginHolder

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Random

abstract class ElasticSearch {
  import ElasticSearch._
  val log = LoggerFactory.getLogger(getClass)

  protected def makeElasticPluginHolder = new AppPluginHolder[ElasticPlugin]()
  private lazy val elasticPlugin = makeElasticPluginHolder
  def client: Client = elasticPlugin.get.client
  def clusterName = elasticPlugin.get.clusterName

  private val jsMapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  def executeAndGet[Resp <: ActionResponse](builder: ActionRequestBuilderResp[Resp]): Resp = {
    @tailrec def doExecute(tries: Int): Resp = {
      try {
        builder.execute().get()
      } catch {
        case e: NoNodeAvailableException =>
          Thread.sleep(Random.nextInt(250))
          if (tries > 1) {
            doExecute(tries - 1)
          } else {
            log.error("No node available in 10 tries for query: " + builder.toString)
            throw e
          }
      }
    }
    doExecute(10)
  }

  /**
    * Сделать http запрос напрямую к elastic-серверу.
    * Например, чтобы задать mapping, нужно вызвать этот метод с параметрами:
    * method=PUT, subPath=/myindex/main/_mapping, data=Some([mapping-json stream])
    *
    * Результат возвращается строкой. Также, метод может бросить FileNotFoundException, IOException.
    */
  def httpRequest(method: HttpMethod, path: String, data: Option[Array[Byte]] = None): String = {
    val es: ElasticPlugin = elasticPlugin.get
    val address = es.transportAddresses.head
    val conn = new URL("http", address.getHost, es.httpPort, path).openConnection().asInstanceOf[HttpURLConnection]
    conn.setRequestMethod(method.toString)
    data.foreach {bytes =>
      conn.setDoOutput(true)
      conn.getOutputStream.write(bytes)
    }
    conn.connect()
    conn.getResponseCode match {
      case 400 => throw new IOException(IOUtils.readString(conn.getErrorStream))
      case _ => IOUtils.readString(conn.getInputStream)
    }
  }

  // ------------------------------- Index methods -------------------------------

  /**
    * Вернуть информацию об индексе в виде yaml.
    */
  def getIndexInfo(index: String): Option[String] =
    try Some(httpRequest(HttpMethod.GET, "/" + index + "?format=yaml"))
    catch {case e: FileNotFoundException => None}

  /** Вернуть настройки индекса */
  def getIndexSettings(index: String): Option[EsIndexSettings] =
    try Some {
      val response: String = httpRequest(HttpMethod.GET, "/" + index + "/_settings")
      val parser: JsonParser = jsMapper.readTree(response).get(index).get("settings").get("index").traverse()
      jsMapper.readValue(parser, classOf[EsIndexSettings])
    }
    catch {case e: FileNotFoundException => None}

  def setNumberOfReplicas(index: String, number: Int): String = {
    val data: String = "{\"index\":{\"number_of_replicas\":" + number + "}}"
    httpRequest(HttpMethod.PUT, "/" + index + "/_settings", Some(data.getBytes))
  }

  /**
    * Удалить и пересоздать индекс.
    */
  def resetAndCreateIndex(index: String, indexBytes: Array[Byte]): String = {
    try {
      httpRequest(HttpMethod.DELETE, "/" + index)
    } catch {
      case e: IOException => () // Индекс не найден - это нормально, продолжаем.
    }
    httpRequest(HttpMethod.PUT, "/" + index, Some(indexBytes))
  }

  // ------------------------------- Mapping methods -------------------------------

  /**
    * Вернуть mapping таблицы в виде yaml.
    */
  def getMapping(metaEs: EsTypeMeta[_]): Option[String] =
    try Some(httpRequest(HttpMethod.GET, metaEs.elasticMapping.baseHttpPath + "/_mapping?format=yaml"))
    catch {case e: FileNotFoundException => None}

  /**
    * Сбросить все данные таблицы и задать ей новый mapping, который будет загружен как внутренний ресурс.
    */
  def resetAndPutMapping(metaEs: EsTypeMeta[_], mappingBytes: Array[Byte]): String = {
    try {
      httpRequest(HttpMethod.DELETE, metaEs.elasticMapping.baseHttpPath)
    } catch {
      case e: IOException => () // Индекс не найден - это нормально, продолжаем.
    }
    httpRequest(HttpMethod.PUT, metaEs.elasticMapping.baseHttpPath + "/_mapping", Some(mappingBytes))
  }

  // ------------------------------- Query methods with logging to PageLog -------------------------------

  def searchScroll(scrollId: String, keepAlive: TimeValue): SearchResponse = {
    val resp: SearchResponse = executeAndGet(client.prepareSearchScroll(scrollId).setScroll(keepAlive))
    PageLog.addEsQuery(resp.getTookInMillis)
    resp
  }

  def clearScroll(scrollId: String) {
    executeAndGet(client.prepareClearScroll().addScrollId(scrollId))
  }
  def clearScrolls(scrollIds: Seq[String]) {
    executeAndGet(client.prepareClearScroll().setScrollIds(scrollIds.asJava))
  }

  def bulkIndex[TR <: TableRecord](items: Iterator[TR], writeFactory: TR => Option[EsTypeWrite], bulkSize: Int = 100): Either[BulkResponse, Unit] = {
    @tailrec
    def doBulk(items: Stream[TR]): Either[BulkResponse, Unit] = {
      val bulkRequest = client.prepareBulk()
      val (curItems, nextItems) = items.splitAt(bulkSize)
      for (item <- curItems) {
        writeFactory(item).foreach(record => bulkRequest.add(record.prepareIndex))
      }
      bulkRequest.execute().actionGet() match {
        case result if result.hasFailures => Left(result)
        case _ => if (nextItems.isEmpty) Right(()) else doBulk(nextItems)
      }
    }
    doBulk(items.toStream)
  }
}


object ElasticSearch extends ElasticSearch {
  /** Настройки индекса эластика
    * При необходимости можно добавить другие поля  */
  @JsonIgnoreProperties(ignoreUnknown = true)
  case class EsIndexSettings(@JsonProperty("number_of_shards") numberOfShards: Int,
                             @JsonProperty("number_of_replicas") numberOfReplicas: Int)


  type ActionRequestBuilderResp[Resp <: ActionResponse] = ActionRequestBuilder[Req, Resp, RB] forSome {
    type Req <: ActionRequest
    type RB <: ActionRequestBuilder[Req, Resp, RB]
  }
}
