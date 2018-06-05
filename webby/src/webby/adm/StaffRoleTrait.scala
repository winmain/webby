package webby.adm
import querio.{AnyTable, TableRecord}

/**
  * Table `adm.staff_role`.
  * Contains staff roles with permissions.
  */
trait StaffRoleTableTrait { self: AnyTable =>
  def id: Int_TF
}

trait StaffRoleTrait extends TableRecord[Int] {
  def id: Int
  def title: String
  def permissions: Set[_ <: OnePermission]
}
