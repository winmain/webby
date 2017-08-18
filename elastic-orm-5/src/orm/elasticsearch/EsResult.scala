package orm.elasticsearch

import org.elasticsearch.search.aggregations.{Aggregation, Aggregations}
import webby.commons.collection.Pager

class EsResult[T](val took: Long,
                  val found: Int,
                  val rows: Vector[T],
                  val esQueryBuilder: Object,
                  val aggs: Option[Aggregations] = None) {
  def esQuery = esQueryBuilder.toString

  def firstAgg[A <: Aggregation]: A = aggs.get.iterator().next().asInstanceOf[A]

  def hasMore(pager: Pager): Boolean = pager.hasMore(found)
}


object EsResult {
  def empty[T] = new EsResult[T](0, 0, Vector.empty[T], esQueryBuilder = "<empty>")
}
