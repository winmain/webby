package webby.adm
import java.security.SecureRandom
import java.time.Instant

import querio.{AnyTable, DbTrait}
import webby.commons.time.StdDates

import scala.annotation.tailrec
import scala.util.Random

trait StaffSessContainerTrait[StaffSess <: StaffSessTrait] {

  def startNewSession(now: Long, staffId: Int, timeoutInSeconds: Int): Long

  def remove(token: Long)

  def get(now: Long, token: Long): Option[StaffSess]

  def setTimeout(now: Long, staffSess: StaffSess, timeoutInSeconds: Int): StaffSess

  /**
    * Remove outdated locks
    */
  def weed(): Unit
}


class StdStaffSessContainer[Adm <: AdmTrait](val adm: Adm) extends StaffSessContainerTrait[Adm#StaffSess] {
  type StaffSess = Adm#StaffSess

  private[adm] val random = new Random(new SecureRandom())

  private val db: DbTrait = adm.db

  override def startNewSession(now: Long, staffId: Int, timeoutInSeconds: Int): Long = {
    val token: Long = generate(now)
    db.dataTrReadUncommittedNoLog {implicit dt =>
      val sess = adm.createNewStaffSess
      sess.staffId = staffId
      sess.token = token
      sess.endTime = Instant.ofEpochMilli(now + timeoutInSeconds * StdDates.Second)
      db.insert(sess)

      db.updatePatchOne(adm.staffTable, staffId)(_.lastLoginOn = Some(Instant.ofEpochMilli(now)))
    }
    token
  }

  override def remove(token: Long) {
    db.dataTrReadUncommittedNoLog {implicit dt =>
      db.deleteByCondition(adm.staffSessTable.asInstanceOf[AnyTable], adm.staffSessTable.token == token)
    }
  }

  override def get(now: Long, token: Long): Option[StaffSess] =
    adm.staffSessCache.byToken(now, token)

  override def setTimeout(now: Long, staffSess: StaffSess, timeoutInSeconds: Int): StaffSess = {
    db.dataTrReadUncommittedNoLog {implicit dt =>
      db.updateRecordPatch(adm.staffSessTable, staffSess.asInstanceOf[adm.StaffSess]) {r =>
        r.endTime = Instant.ofEpochMilli(now + timeoutInSeconds * StdDates.Second)
      }.toRecord
    }
  }

  override def weed(): Unit = {
    db.dataTrReadUncommittedNoLog {implicit dt =>
      db.deleteByCondition(adm.staffSessTable, adm.staffSessTable.endTime < Instant.ofEpochMilli(System.currentTimeMillis()))
    }
  }

  //
  // ------------------------------- Private & protected methods -------------------------------
  //
  @tailrec private[adm] final def generate(now: Long): Long = {
    val token: Long = random.nextLong() & 0x7fffffffffffffffL // Только положительные числа
    if (get(now, token).isDefined) generate(now) else token
  }
}
