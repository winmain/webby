package orm.elasticsearch

import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get.{GetRequestBuilder, GetResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.index.{IndexRequestBuilder, IndexResponse}
import org.elasticsearch.action.search._
import org.elasticsearch.client.Client
import org.slf4j.LoggerFactory
import webby.commons.system.log.PageLog

/**
  * Класс для выполнения прямых запросов к Эластику для заданного индекса и типа (index, tpe).
  */
class ElasticIndexMapping(val client: Client, val index: String, val tpe: String = "main") {
  import ElasticSearch.executeAndGet

  val log = LoggerFactory.getLogger(getClass)

  @inline protected def withPageLog[R](query: => R): R = {
    val t0 = System.currentTimeMillis()
    val resp = query
    val time = System.currentTimeMillis() - t0
    PageLog.addEsQuery(time)
    resp
  }

  def baseHttpPath: String = "/" + index + "/" + tpe

  def rawPrepareIndex(id: String): IndexRequestBuilder =
    client.prepareIndex(index, tpe, id)

  def index(id: String)(block: IndexRequestBuilder => Any): IndexResponse = {
    val builder = client.prepareIndex(index, tpe, id)
    block(builder)
    withPageLog(executeAndGet(builder))
  }

  def delete(id: String)(block: DeleteRequestBuilder => Any): DeleteResponse = {
    val builder = client.prepareDelete(index, tpe, id)
    block(builder)
    withPageLog(executeAndGet(builder))
  }

  def count(block: SearchRequestBuilder => Any): Long =
    search(rb => block(rb.setSize(0))).getHits.getTotalHits

  def search(block: SearchRequestBuilder => Any): SearchResponse = {
    val builder = client.prepareSearch(index).setTypes(tpe)
    block(builder)
    val resp: SearchResponse = executeAndGet(builder)
    PageLog.addEsQuery(resp.getTook.millis())
    resp
  }

  def multiSearch(block: MultiSearchRequestBuilder => Any): MultiSearchResponse = {
    val builder = client.prepareMultiSearch()
    block(builder)
    val resp: MultiSearchResponse = executeAndGet(builder)
    var time = 0L
    for (item <- resp.getResponses) {
      if (item.getResponse != null) time += item.getResponse.getTook.millis()
    }
    PageLog.addEsQuery(time)
    resp
  }

  def get(id: String)(block: GetRequestBuilder => Any): GetResponse = {
    val builder = client.prepareGet(index, tpe, id)
    block(builder)
    withPageLog(executeAndGet(builder))
  }

  def multiGet(block: MultiGetRequestBuilder => Any): MultiGetResponse = {
    val builder = client.prepareMultiGet
    block(builder)
    withPageLog(executeAndGet(builder))
  }

  def searchScroll(scrollId: String, block: SearchScrollRequestBuilder => Any): SearchResponse = {
    val builder = client.prepareSearchScroll(scrollId)
    block(builder)
    withPageLog(executeAndGet(builder))
  }

  def clearScroll(block: ClearScrollRequestBuilder => Any): ClearScrollResponse = {
    val builder = client.prepareClearScroll()
    block(builder)
    withPageLog(executeAndGet(builder))
  }
}
