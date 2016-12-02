package webby.adm
import java.sql.Timestamp

import querio.{AnyTable, MutableTableRecord, TableRecord}

/**
  * Table `adm.staff`.
  * Contains staff users.
  */
trait StaffTableTrait { self: AnyTable =>
  def id: Int_TF
}

trait StaffTrait extends TableRecord {
  def id: Int
  def staffRoleId: Option[Int]
  def login: String
  def hashedPassword: Option[String]
  def active: Boolean
  def adm: Boolean
  def addPermissions: Set[_ <: OnePermission]
  def createdOn: Timestamp
  def lastLoginOn: Option[Timestamp]
  def fullname: String
}

trait MutableStaffTrait[TR <: StaffTrait] extends MutableTableRecord[TR] {
  var id: Int
  var lastLoginOn: Option[Timestamp]
}
