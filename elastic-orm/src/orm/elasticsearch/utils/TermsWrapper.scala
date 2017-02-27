package orm.elasticsearch.utils
import org.elasticsearch.search.aggregations.bucket.terms.Terms

import scala.collection.JavaConverters._

class TermsWrapper(terms: Terms) {
  def totalCountLong: Long = terms.getBuckets.asScala.foldLeft(0L)(_ + _.getDocCount) + terms.getSumOfOtherDocCounts
  def totalCount: Int = totalCountLong.toInt
}
