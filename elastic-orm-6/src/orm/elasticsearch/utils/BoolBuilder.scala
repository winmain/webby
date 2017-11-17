package orm.elasticsearch.utils

import org.elasticsearch.index.query.{BoolQueryBuilder, MatchAllQueryBuilder, QueryBuilder}

/**
  * Класс для удобного создания [[BoolQueryBuilder]] с человеческим интерфейсом.
  * На выходе необязательно будет [[BoolQueryBuilder]]. Если нет фильтров, то [[BoolQueryBuilder]]
  * не будет создан.
  */
case class BoolBuilder() {

  private var _bool: BoolQueryBuilder = null
  private var _query: QueryBuilder = null
  private var _and: QueryBuilder = null
  private var _or: QueryBuilder = null

  def isEmpty: Boolean = _bool == null && _query == null && _and == null && _or == null
  def nonEmpty: Boolean = !isEmpty

  def query(query: QueryBuilder): BoolBuilder = {
    if (needBoolToAddClause) ensureBool.must(query)
    else if (_bool != null) _bool.must(query)
    else _query = query
    this
  }

  def and(filter: QueryBuilder): BoolBuilder = {
    if (needBoolToAddClause) ensureBool.filter(filter)
    else if (_bool != null) _bool.filter(filter)
    else _and = filter
    this
  }

  def or(filter: QueryBuilder): BoolBuilder = {
    if (needBoolToAddClause) ensureBool.minimumShouldMatch("1").should(filter)
    else if (_bool != null) _bool.minimumShouldMatch("1").should(filter)
    else _or = filter
    this
  }

  def not(filter: QueryBuilder): BoolBuilder = {ensureBool.mustNot(filter); this}

  /**
    * Returns optimized result:
    * If only query defined in [[query()]] returns it.
    * If filters defined returns [[BoolQueryBuilder]].
    * If nothing defined returns [[MatchAllQueryBuilder]].
    */
  def result: QueryBuilder = {
    if (_bool != null) _bool
    else {
      if (_query != null) _query
      else if (_and != null) new BoolQueryBuilder().filter(_and)
      else if (_or != null) new BoolQueryBuilder().filter(_or)
      else new MatchAllQueryBuilder
    }
  }

  /**
    * Always returns [[BoolQueryBuilder]].
    * For optimized result use [[result]] method.
    */
  def resultBool: BoolQueryBuilder = ensureBool

  // ------------------------------- Additional helper methods -------------------------------

  def and(maybeFilter: Option[QueryBuilder]): BoolBuilder = maybeFilter match {
    case Some(filter) => and(filter)
    case None => this
  }

  def and(filters: Seq[QueryBuilder]): BoolBuilder = {filters.foreach(and); this}
  def and(boolBuilder: BoolBuilder): BoolBuilder = and(boolBuilder.result)

  def or(maybeFilter: Option[QueryBuilder]): BoolBuilder = maybeFilter match {
    case Some(filter) => or(filter)
    case None => this
  }

  def or(filters: Seq[QueryBuilder]): BoolBuilder = {filters.foreach(or); this}
  def or(boolBuilder: BoolBuilder): BoolBuilder = or(boolBuilder.result)

  def not(maybeFilter: Option[QueryBuilder]): BoolBuilder = maybeFilter match {
    case Some(filter) => not(filter)
    case None => this
  }
  def not(boolBuilder: BoolBuilder): BoolBuilder = not(boolBuilder.result)

  // ------------------------------- Private & protected methods -------------------------------

  private def needBoolToAddClause: Boolean = _query != null || _and != null || _or != null

  private def ensureBool: BoolQueryBuilder = {
    if (_bool != null) _bool
    else {
      _bool = new BoolQueryBuilder
      if (_query != null) {_bool.must(_query); _query = null}
      if (_and != null) {_bool.filter(_and); _and = null}
      if (_or != null) {_bool.minimumShouldMatch("1").should(_or); _or = null}
      _bool
    }
  }
}
