package webby.commons.concurrent

import org.slf4j.LoggerFactory

/**
 * Класс, предотвращающий многопоточные запуски определённого процесса.
 * Полезно для крон-заданий.
 *
 * Пример:
 * {{{
 *  object ResSubscribeSender {
 *    private val runner = new SingleRun(this)
 *
 *    def sendAll() {
 *      runner.run("sendAll") {
 *        // do task
 *      }
 *    }
 *  }
 * }}}
 * В данном случае, если метод sendAll() не успевает отработать за отведённое время, и всё ещё
 * выполняется, то при следующем вызове этого метода по крону, он не будет вызван, кроме того
 * данный случай будет залогирован.
 * Эта обёртка гарантирует одновременный запуск не более одного задания.
 *
 * @param parentClass Класс, от которого будет запись в логе
 */
class SingleRun(parentClass: Class[_]) {
  def this(parentObj: AnyRef) = this(parentObj.getClass)

  @volatile private var running: Boolean = false

  def run[R](name: String)(block: => Any) {
    if (running) {
      LoggerFactory.getLogger(parentClass).warn("Still run in " + name)
    } else {
      running = true
      try block
      finally {running = false}
    }
  }
}
