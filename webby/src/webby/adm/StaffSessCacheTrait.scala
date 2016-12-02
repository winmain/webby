package webby.adm

import querio.{AnyTable, TrDeleteChange, TrRecordChange, TrSomeChange}
import webby.commons.cache.table.AbstractRecordsCache

import scala.collection.immutable.{IntMap, LongMap}

trait StaffSessCacheTrait[StaffSess <: StaffSessTrait] extends AbstractRecordsCache {

  def byId(now: Long, id: Int): Option[StaffSess]

  def byToken(now: Long, token: Long): Option[StaffSess]
}


class StdStaffSessCache[Adm <: AdmTrait](val adm: Adm) extends StaffSessCacheTrait[Adm#StaffSess] {
  type StaffSess = adm.StaffSess
  override def dbTable: AnyTable = adm.staffSessTable

  protected var partialIdCache: IntMap[StaffSess] = IntMap.empty
  protected var partialTokenCache: LongMap[StaffSess] = LongMap.empty

  override def byId(now: Long, id: Int): Option[StaffSess] = partialIdCache.get(id).orElse {
    val maybeRecord: Option[StaffSess] = readRecordById(now, id)
    maybeRecord.foreach(putRecord)
    maybeRecord
  }.filter(_.endTime.getTime > now)

  override def byToken(now: Long, token: Long): Option[StaffSess] = partialTokenCache.get(token).orElse {
    val maybeRecord: Option[StaffSess] = readRecordByToken(now, token)
    maybeRecord.foreach(putRecord)
    maybeRecord
  }.filter(_.endTime.getTime > now)

  override def resetCache(): Unit = {
    partialIdCache = IntMap.empty
    partialTokenCache = LongMap.empty
    super.resetCache()
  }

  /**
    * Reset value for one record, i.e. recalculate hash for the record.
    */
  override def resetRecord(id: Int, change: TrRecordChange): Unit = {
    def deleteChange(id: Int): Unit = {
      partialIdCache.get(id).foreach {record =>
        partialIdCache -= record.id
        partialTokenCache -= record.token
      }
    }

    change match {
      case some: TrSomeChange => putRecord(some.mtr.toRecord.asInstanceOf[StaffSess])
      case del: TrDeleteChange => deleteChange(id)
      case _ =>
        readRecordById(System.currentTimeMillis(), id) match {
          case Some(record) => putRecord(record)
          case None => deleteChange(id)
        }
    }
    super.resetRecord(id, change)
  }

  protected def readRecordById(now: Long, id: Int): Option[StaffSess] =
    adm.db.findById(adm.staffSessTable, id).filter(_.endTime.getTime > now)

  protected def readRecordByToken(now: Long, token: Long): Option[StaffSess] =
    adm.db.findByField(adm.staffSessTable.token, token).filter(_.endTime.getTime > now)

  protected def putRecord(record: StaffSess): Unit = {
    partialIdCache = partialIdCache.updated(record.id, record)
    partialTokenCache = partialTokenCache.updated(record.token, record)
  }
}
