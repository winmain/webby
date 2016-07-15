package webby.commons.collection

import webby.api.mvc.{PlainResult, Results}

trait PagerOrViewAll

object ViewAll extends PagerOrViewAll

/**
  * Листалка страниц для удобного составления запросов
  *
  * Requires sbt dependency
  * {{{
  *   deps += "com.github.winmain" %% "querio" % querioVersion
  * }}}
  *
  * @param step Шаг листалки (обычно 10, 25 элементов)
  * @param page Страница, начиная с 1.
  */
class Pager(val step: Int, val page: Int = 1) extends querio.utils.Pager with PagerOrViewAll {
  def numberOfRows: Int = step
  def offset: Int = (page - 1) * step

  def helper(totalCount: Int, nearRadius: Int): PagerHelper = new PagerHelper(page, step, totalCount, nearRadius)
  def helperUnknownTotal(currentCount: Int): PagerHelper = {
    var totalCount: Int = (page - 1) * step + currentCount
    if (currentCount == step) totalCount += 1
    new PagerHelper(page, step, totalCount, 1)
  }

  def hasMore(totalCount: Int): Boolean = totalCount > page * step

  /** Находимся ли мы на последней странице? */
  def isLastPage(totalCount: Int): Boolean = page * step >= totalCount

  def isInLimit(maxAllowedValue: Int): Boolean = page * step <= maxAllowedValue
  def checkLimit(maxAllowedValue: Int) {
    require(isInLimit(maxAllowedValue), "Pager limit overflow. Got: " + page * step + ", must be <= " + maxAllowedValue)
  }
}

object Pager {
  val default10 = new Pager(10)
  val default25 = new Pager(25)

  def apply(step: Int, page: Int) = new Pager(step, page)

  def isValidPage(page: Int): Boolean = page >= 1

  def withPage(step: Int, page: Int)(block: Pager => PlainResult): PlainResult =
    if (isValidPage(page)) block(new Pager(step, page))
    else Results.NotFoundRaw

  def calcPages(step: Int, totalCount: Int): Int =
    totalCount / step + (if ((totalCount % step) > 0) 1 else 0)
}


/**
  * Класс-помощник, используемый при создании html листалки. Он помогает определить количество
  * выводимых страниц.
  *
  * @param page       Номер страницы
  * @param step       Шаг листания
  * @param totalCount Общее количество элементов
  * @param nearRadius Количество дополнительных страниц по каждую сторону от выбранной.
  *                   Например, при radius = 2 последовательность может быть такой: 2, 3, *4*, 5, 6.
  */
class PagerHelper(val page: Int, val step: Int, val totalCount: Int, val nearRadius: Int) {
  /** Количество страниц для заданного количества объектов */
  val pages: Int = Pager.calcPages(step, totalCount)

  val start = math.max(1, page - nearRadius)
  val end = math.min(pages, page + nearRadius)

  def startDots = start > 1
  def endDots = end < pages
  def range = Range(start, end + 1)

  def hasBack = page > 1
  def hasNext = page < pages
}
