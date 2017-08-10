package orm.elasticsearch.utils

import org.elasticsearch.search.aggregations.{Aggregation, Aggregations}

class AggregationsWrapper(agg: Aggregations) {
  def first[A <: Aggregation]: A = agg.iterator().next().asInstanceOf[A]
}
