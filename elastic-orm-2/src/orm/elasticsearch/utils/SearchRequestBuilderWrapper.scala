package orm.elasticsearch.utils
import org.elasticsearch.action.search.SearchRequestBuilder
import webby.commons.collection.{Pager, PagerOrViewAll, ViewAll}

class SearchRequestBuilderWrapper(search: SearchRequestBuilder) {
  def pager(pager: PagerOrViewAll, limit: Int): Unit = pager match {
    case p: Pager =>
      p.checkLimit(limit)
      if (p.page > 1) search.setFrom((p.page - 1) * p.step)
      search.setSize(p.step)

    case ViewAll =>
      search.setSize(limit)
  }
}
