package webby.adm
import java.time.Instant

import querio.{AnyTable, MutableTableRecord, TableRecord}

/**
  * Table `adm.staff_sess`.
  * Contains staff user sessions with tokens and expire time.
  */
trait StaffSessTableTrait { self: AnyTable =>
  def id: Int_TF
  def token: Long_TF
  def staffId: Int_TF
  def endTime: Instant_TF
}

trait StaffSessTrait extends TableRecord[Int] {
  def id: Int
  def token: Long
  def staffId: Int
  def endTime: Instant

  def adm: AdmTrait

  val staff: AdmTrait#Staff = adm.staffCache.get(staffId).get
  val role: Option[AdmTrait#StaffRole] = staff.staffRoleId.flatMap(adm.staffRoleCache.get(_))

  lazy val permissions: Set[_ <: OnePermission] =
    (role match {
      case Some(r) => r.permissions
      case None => Set.empty
    }) ++ staff.addPermissions

  def canAccessTo(perm: PermissionSet) = perm.canAccess(permissions)
}

trait MutableStaffSessTrait[TR <: StaffSessTrait] extends MutableTableRecord[Int, TR] {
  var id: Int
  var token: Long
  var staffId: Int
  var endTime: Instant
}
