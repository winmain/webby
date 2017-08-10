package orm.elasticsearch

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.Terms

package object utils {
  import scala.language.implicitConversions

  implicit def aggregationsWrapper(agg: Aggregations): AggregationsWrapper = new AggregationsWrapper(agg)
  implicit def searchRequestBuilderWrapper(search: SearchRequestBuilder): SearchRequestBuilderWrapper = new SearchRequestBuilderWrapper(search)
  implicit def termsWrapper(terms: Terms): TermsWrapper = new TermsWrapper(terms)

  def boolBuilder: BoolBuilder = new BoolBuilder
}
