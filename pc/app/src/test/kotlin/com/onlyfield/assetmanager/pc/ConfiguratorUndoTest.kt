package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.forms.*
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ConfiguratorUndoTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun composedRackDevicesAndCableSaveReopenAndUndoTogether() {
        val state = DesktopAppState(DesktopStorageManager(folder.newFolder()))
        val reopened = DesktopAppState(DesktopStorageManager(folder.newFolder()))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Site", sites = listOf(
                Site(name = "BU", areas = listOf(Area(name = "Floor")))), password = "test", passwordConfirm = "test")))
            val original = requireNotNull(state.project)
            val site = original.sites.single()
            val rackDraft = MapObjectDraft.forRack(original, null).let { it.copy(areaId = site.areas.single().id,
                rack = it.rack.copy(name = "Rack", areaId = site.areas.single().id)) }
            var staged = rackDraft.apply(original)
            val parent = ObjectRef(PlacementTargetType.RACK, rackDraft.id)
            val type = ObjectCatalog.builtins.first { it.id == "switch" }
            val drafts = (1..2).map { number -> MapObjectDraft.newObject(staged, type, site.id, site.areas.single().id, parent).let {
                it.copy(device = it.device.copy(technicalName = "SW$number", positionU = number.toString(),
                    hardware = HardwareSpec(portGroups = listOf(PortTemplate("P", portCount = 24), PortTemplate("SFP", portCount = 4, connector = "SFP")))))
            } }
            drafts.forEach { staged = it.apply(staged) }
            val devices = staged.sites.single().devices
            staged = HardwareConfigurator.connect(staged, devices[0].ports.first().id, devices[1].ports.first().id, CableMedium.ETHERNET_COPPER)
            val configured = rackDraft.copy(session = ConfigurationSession(original, staged)).apply(original)
            state.update(configured, "Configurator")
            assertNull(state.error)
            assertTrue(state.canUndo)
            val snapshot = folder.newFile("configured.ofam").apply { writeBytes(state.storedProjects.single().file.readBytes()) }
            reopened.importFile(snapshot, "test", compare = false)
            assertNull(reopened.error)
            assertEquals(configured, reopened.project)
            assertEquals(ConnectionState.COMPLETE, ConnectionGraph(requireNotNull(reopened.project)).state(devices[0].ports.first().id))
            state.undo()
            assertNull(state.error)
            assertEquals(original, state.project)
            assertFalse(state.canUndo)
            assertTrue(requireNotNull(state.project).cables.isEmpty())
            assertTrue(requireNotNull(state.project).objectContainments.isEmpty())
        } finally { state.shutdown(); reopened.shutdown() }
    }
}
