package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test

class NetworkScopeRetentionTest {
    private val survivor = Device(technicalName = "Survivor")
    private val duplicate = Device(technicalName = "Duplicate")
    private val site = Site(name = "Devices", devices = listOf(survivor, duplicate))
    private val emptySite = Site(name = "Empty")
    private val project = Project(name = "Scopes", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(site, emptySite))
    private fun scoped(type: VlanScopeType, target: String, subnet: Boolean): Project = if (subnet)
        project.copy(subnets = listOf(Subnet(cidrBlock = "10.0.0.0/24", scopeType = type, scopeTargetId = target))) else
        project.copy(vlans = listOf(Vlan(vlanId = 10, name = "LAN", scopeType = type, scopeTargetId = target)))

    @Test fun siteDeletionWaitsForExplicitRemovalOfVlanAndSubnetScopes() {
        for (subnet in listOf(false, true)) {
            val original = scoped(VlanScopeType.SITE, emptySite.id, subnet)
            assertNull(ProjectEdits.deleteSite(original, emptySite.id))
            assertEquals(listOf(site, emptySite), original.sites)
            val released = original.copy(vlans = emptyList(), subnets = emptyList())
            val deleted = ProjectEdits.deleteSite(released, emptySite.id)!!
            assertEquals(listOf(site), deleted.sites)
            exchange(original); exchange(deleted)
        }
    }

    @Test fun deviceTrashReplacementAndMergeRefuseReferencedTargetsWithoutChangingNetworks() {
        for (subnet in listOf(false, true)) for (target in listOf(survivor.id, duplicate.id)) {
            val original = scoped(VlanScopeType.DEVICE, target, subnet)
            assertThrows(IllegalStateException::class.java) { ProjectEdits.deleteDeviceToTrash(original, target) }
            assertThrows(IllegalStateException::class.java) { ProjectEdits.replaceDevice(original, target, "Replacement", DeviceCategory.CUSTOM) }
            assertThrows(IllegalStateException::class.java) { ProjectEdits.mergeDevices(original, survivor.id, duplicate.id, MergeDataChoices()) }
            assertEquals(project.sites, original.sites)
            val unrelated = if (target == survivor.id) duplicate.id else survivor.id
            val (deleted, item) = ProjectEdits.deleteDeviceToTrash(original, unrelated)
            assertNotNull(item)
            assertEquals(original.vlans, deleted.vlans); assertEquals(original.subnets, deleted.subnets)
            exchange(original); exchange(deleted)
        }
    }

    @Test fun explicitScopeRemovalAllowsTrashRestoreAndMerge() {
        val released = scoped(VlanScopeType.DEVICE, duplicate.id, true).copy(subnets = emptyList())
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(released, duplicate.id)
        val restored = ProjectEdits.restoreFromTrash(deleted, item!!)
        assertEquals(released.copy(updatedEpochMs = restored.updatedEpochMs), restored)
        val (merged, mergeItem) = ProjectEdits.mergeDevices(restored, survivor.id, duplicate.id, MergeDataChoices())
        assertNotNull(mergeItem)
        assertEquals(listOf(survivor.id), merged.sites.first().devices.map { it.id })
        exchange(merged)
    }

    private fun exchange(project: Project) {
        for (password in listOf(null, "dummy-password")) {
            val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(project, password = password), password)
            assertTrue(result.validationResult.isValid)
            result.pkg!!.use { assertEquals(project, it.project) }
        }
    }
}
