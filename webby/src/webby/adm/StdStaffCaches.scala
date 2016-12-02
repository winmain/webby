package webby.adm
import webby.commons.cache.table.TableCache

trait StdStaffCaches { adm: AdmTrait =>

  class StdStaffCache extends TableCache(db, staffTable) {
    // We should reset all staff session cache because of cached values of StaffSess.staff and StaffSess.permissions
    addResetAnyListener(staffSessCache.resetCache())

    /**
      * Find all staff with specified permission.
      */
    def listWithPermission(perm: OnePermission): Iterable[Staff] = {
      val roleIds = staffRoleCache.allRecords.withFilter(_.permissions.exists(_ == perm)).map(_.id).toVector
      allRecords.filter(_.staffRoleId.exists(roleIds.contains))
    }
  }


  class StdStaffRoleCache extends TableCache[StaffRole](db, staffRoleTable) {
    // We should reset all staff session cache because of cached values of StaffSess.staff and StaffSess.permissions
    addResetAnyListener(staffSessCache.resetCache())
  }
}
