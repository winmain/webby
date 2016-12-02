package webby.adm

import webby.commons.collection.IterableWrapper.wrapIterable

/**
  * Basic trait for permission enum.
  *
  * Sample implementation:
  * {{{
  * object Permission extends ScalaDbEnum[Permission] {
  *   type V = Permission
  *
  *   def Any = AnyPermission
  *
  *   val PATCH = new V("patch", "Ability to run one-shoot patches")
  * }
  *
  * case class Permission private(dbValue: String,
  *                               comment: String)
  *   extends ScalaDbEnumCls[Permission](Permission, dbValue) with PermissionSet with OnePermission {
  *
  *   override def dbValues: String = dbValue
  * }
  * }}}
  */
trait OnePermission {
  def canAccess[P <: OnePermission](userPerms: Set[P]): Boolean = userPerms.contains(this.asInstanceOf[P])
}

trait PermissionSet {
  def canAccess[P <: OnePermission](userPerms: Set[P]): Boolean
  def &(other: PermissionSet): AndPermissions = AndPermissions(Seq(this, other))
  def |(other: PermissionSet): OrPermissions = OrPermissions(Seq(this, other))

  def dbValues: String
}

object AnyPermission extends PermissionSet {
  override def canAccess[P <: OnePermission](userPerms: Set[P]): Boolean = true
  override def dbValues: String = "[any]"
}

case class AndPermissions(values: Seq[PermissionSet]) extends PermissionSet {
  override def canAccess[P <: OnePermission](userPerms: Set[P]): Boolean = values.forall(_.canAccess(userPerms))
  override def &(other: PermissionSet): AndPermissions = AndPermissions(other +: values)
  override def dbValues: String = values.mapMkString(_.dbValues, "(", " & ", ")")
}

case class OrPermissions(values: Seq[PermissionSet]) extends PermissionSet {
  override def canAccess[P <: OnePermission](userPerms: Set[P]): Boolean = values.exists(_.canAccess(userPerms))
  override def |(other: PermissionSet): OrPermissions = OrPermissions(other +: values)
  override def dbValues: String = values.mapMkString(_.dbValues, "(", " | ", ")")
}
