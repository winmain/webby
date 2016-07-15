package orm.elasticsearch.utils
import org.elasticsearch.search.aggregations.bucket.terms.Terms

import scala.collection.JavaConversions._

class TermsWrapper(terms: Terms) {
  def totalCountLong: Long = terms.getBuckets.foldLeft(0L)(_ + _.getDocCount) + terms.getSumOfOtherDocCounts
  def totalCount: Int = totalCountLong.toInt
}
