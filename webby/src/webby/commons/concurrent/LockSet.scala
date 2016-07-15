package webby.commons.concurrent

import java.util.concurrent.locks.Lock

import com.google.common.util.concurrent.Striped

import scala.annotation.tailrec
import scala.util.Random

/**
 * Класс, служащий для ограничения доступа к какому-либо ресурсу по ключу.
 * Например, если нужно добавить запись в БД с уникальным ключом, но только одну, например,
 * это может быть отклик резюме на вакансию, где в качестве ключа идёт tuple (id_res, id_vac).
 *
 * По сути, это обёртка над Striped[Lock], созданная для удобной работы с ним в scala.
 * Его лучше инициализировать через объект LockSet.
 *
 * @param striped Внутреннее хранилище.
 * @tparam K Тип ключей.
 */
class LockSet[K](striped: Striped[Lock]) {

  def withLock[R](key: K)(synchronizedBlock: => R): R = {
    val lock: Lock = synchronized(striped.get(key))
    try {
      lock.lock()
      synchronizedBlock
    }
    finally lock.unlock()
  }

  def withDoubleLock[R](key1: K, key2: K, maxAttempts: Int = 10)(synchronizedBlock: => R): R = {
    val (lock1: Lock, lock2: Lock) = synchronized(striped.get(key1) -> striped.get(key2))
    @tailrec
    def doAttempt(attempts: Int): R = {
      lock1.lock()
      if (lock2.tryLock()) {
        try {
          synchronizedBlock
        } finally {
          lock2.unlock()
          lock1.unlock()
        }
      } else {
        lock1.unlock()
        if (attempts <= 1) sys.error(s"Cannot get double lock for keys $key1, $key2 in $maxAttempts attempts")
        Thread.sleep(Random.nextInt(300))
        doAttempt(attempts - 1)
      }
    }
    doAttempt(maxAttempts)
  }
}


object LockSet {
  /**
   * Creates a LockSet with eagerly initialized, strongly referenced locks, with the
   * specified fairness. Every lock is reentrant.
   *
   * @param stripes the minimum number of stripes (locks) required
   * @return a new { @code Striped<Lock>}
   */
  def lock[K](stripes: Int) = new LockSet[K](Striped.lock(stripes))

  /**
   * Creates a LockSet with lazily initialized, weakly referenced locks, with the
   * specified fairness. Every lock is reentrant.
   *
   * @param stripes the minimum number of stripes (locks) required
   * @return a new { @code Striped<Lock>}
   */
  def lazyWeakLock[K](stripes: Int) = new LockSet[K](Striped.lazyWeakLock(stripes))
}
