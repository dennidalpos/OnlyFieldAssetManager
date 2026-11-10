package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.forms.FieldValidators
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.validation.*
import org.junit.Assert.*
import org.junit.Test

class NetworkValidationTest {
    @Test fun referencedVlanDeletionIsRefusedWithoutMutatingTheProject() {
        val subnet = Subnet(cidrBlock = "10.0.0.0/24", vlanId = vlan.id)
        val original = project.copy(subnets = listOf(subnet))
        assertThrows(IllegalArgumentException::class.java) { com.onlyfield.assetmanager.core.edit.ProjectEdits.deleteVlan(original, vlan.id) }
        assertEquals(listOf(subnet), original.subnets)
        assertEquals(listOf(vlan), original.vlans)
        val disconnected = original.copy(subnets = listOf(subnet.copy(vlanId = null)))
        val deleted = com.onlyfield.assetmanager.core.edit.ProjectEdits.deleteVlan(disconnected, vlan.id)
        assertTrue(deleted.vlans.isEmpty())
        assertEquals(disconnected.subnets, deleted.subnets)
        for (password in listOf(null, "dummy-password")) {
            for (incoming in listOf(original, deleted)) PackageSerializer.importPackage(
                PackageSerializer.exportPackage(incoming, password = password), password).pkg!!.use { assertEquals(incoming, it.project) }
        }
    }
    private val device = Device(technicalName = "Switch")
    private val site = Site(name = "Building", devices = listOf(device))
    private val vlan = Vlan(vlanId = 10, name = "LAN", scopeType = VlanScopeType.DEVICE, scopeTargetId = device.id)
    private val project = Project(name = "Network", createdEpochMs = 0, updatedEpochMs = 0, sites = listOf(site), vlans = listOf(vlan))
    private fun issue(project: Project, code: String) = ModelValidator.validateProject(project).issues.single { it.code == code }
    @Test fun cidrUsesTheSameLimitsAsTheForm() {
        for (cidr in listOf("0.0.0.0/0", "255.255.255.255/32", "10.0.0.0/24", "not-an-address/99", "256.1.1.1/24", "1.2.3.4/-1", "1.2.3.4/33", "", "1.2.3.4/24/1")) {
            val incoming = project.copy(subnets = listOf(Subnet(cidrBlock = cidr, vlanId = vlan.id)))
            assertEquals(cidr, FieldValidators.cidr(cidr, required = true) == null, ModelValidator.validateProject(incoming).isValid)
        }
    }
    @Test fun foreignReferencesAreStructuralAndMissingOptionalDataIsDocumentary() {
        assertEquals(ValidationSeverity.STRUCTURAL_ERROR, issue(project.copy(vlans = listOf(vlan.copy(scopeTargetId = site.id))), "INVALID_NETWORK_SCOPE_TARGET").severity)
        assertEquals(ValidationSeverity.STRUCTURAL_ERROR, issue(project.copy(subnets = listOf(Subnet(cidrBlock = "10.0.0.0/24", vlanId = "foreign"))), "INVALID_SUBNET_VLAN_REFERENCE").severity)
        val incomplete = project.copy(vlans = listOf(vlan.copy(scopeTargetId = null)), subnets = listOf(Subnet(cidrBlock = "10.0.0.0/24", scopeType = VlanScopeType.SITE)))
        assertTrue(ModelValidator.validateProject(incomplete).isValid)
        assertEquals(2, ModelValidator.validateProject(incomplete).issues.count { it.code == "MISSING_NETWORK_SCOPE_TARGET" })
        assertTrue(ModelValidator.validateProject(incomplete).issues.filter { it.code == "MISSING_NETWORK_SCOPE_TARGET" }.all { it.severity == ValidationSeverity.DOCUMENTARY_WARNING })
        for (scope in VlanScopeType.entries) {
            val target = when (scope) { VlanScopeType.PROJECT -> project.id; VlanScopeType.SITE -> site.id; VlanScopeType.DEVICE -> device.id }
            assertTrue(ModelValidator.validateProject(project.copy(vlans = listOf(vlan.copy(scopeType = scope, scopeTargetId = target)))).isValid)
            val subnet = Subnet(cidrBlock = "10.0.0.0/24", scopeType = scope, scopeTargetId = target)
            assertTrue(ModelValidator.validateProject(project.copy(subnets = listOf(subnet))).isValid)
            assertEquals(ValidationSeverity.STRUCTURAL_ERROR, issue(project.copy(subnets = listOf(subnet.copy(scopeTargetId = "foreign"))), "INVALID_NETWORK_SCOPE_TARGET").severity)
        }
    }
    @Test fun simpleAndProtectedImportsRejectInvalidNetworkBeforeReturningAPackage() {
        for (password in listOf(null, "dummy-password")) for (incoming in listOf(
            project.copy(subnets = listOf(Subnet(cidrBlock = "not-an-address/99"))),
            project.copy(vlans = listOf(vlan.copy(scopeTargetId = "foreign"))),
            project.copy(subnets = listOf(Subnet(cidrBlock = "10.0.0.0/24", vlanId = "foreign"))))) {
            val result = PackageSerializer.importPackage(PackageSerializer.exportPackage(incoming, password = password), password)
            assertNull(result.pkg)
            assertFalse(result.validationResult.isValid)
        }
        for (lang in listOf("it", "en", "es")) {
            val i18n = Messages(java.util.Locale.forLanguageTag(lang))
            assertFalse(i18n.text("network.scopeInvalid").contains("network.scopeInvalid"))
        }
    }
}
