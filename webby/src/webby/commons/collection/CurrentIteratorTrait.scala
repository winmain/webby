package webby.commons.collection

/**
 * Итератор, поддерживающий текущее значение (в переменной _cur)
 * @tparam A
 */
trait CurrentIteratorTrait[A] {
  private var _initCur: Boolean = false
  private var _hasCur: Boolean = false
  private var _hasNext: Boolean = false
  private var _cur: A = _

  protected def it: Iterator[A]

  protected def hasN: Boolean = {
    if (!_initCur) advance()
    _hasNext
  }
  protected def hasCur: Boolean = {
    if (!_initCur) advance()
    _hasCur
  }
  protected def cur: A = {
    if (!_initCur) advance()
    _cur
  }

  protected def advance() {
    if (_initCur) {
      _hasCur = _hasNext
      if (_hasCur) {
        _cur = it.next()
        _hasNext = it.hasNext
      }
    } else {
      _hasCur = it.hasNext
      if (_hasCur) {
        _cur = it.next()
        _hasNext = it.hasNext
      } else {
        _hasNext = false
      }
      _initCur = true
    }
  }
}
