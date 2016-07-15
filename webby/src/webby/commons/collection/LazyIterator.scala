package webby.commons.collection

/**
  * Итератор, который получает каждое следующее из `producer`
  *
  * @param producer Источник значений. Если следующее значение есть, он должен вернуть Some(value), если нет, то None.
  */
class LazyIterator[A](producer: => Option[A]) extends Iterator[A] {
  private var cur: Option[A] = None
  private var nextResolved: Boolean = false

  override def hasNext: Boolean = {
    checkNext()
    cur.isDefined
  }

  override def next(): A = {
    checkNext()
    nextResolved = false
    cur.get
  }

  private def checkNext(): Unit = {
    if (!nextResolved) {
      cur = producer
      nextResolved = true
    }
  }
}
