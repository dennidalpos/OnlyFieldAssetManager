package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class DesktopAppStateTest {

    private fun newState() = DesktopAppState(DesktopStorageManager(Files.createTempDirectory("ofam_state").toFile()))

    private fun site(area: String, password: String = "") = NewSiteWizard(
        draft = NewSiteDraft(projectName = "Prova", businessUnit = "Sede", area = area, password = password, passwordConfirm = password)
    )

    @Test
    fun wizardCreatesProtectedProject() {
        val state = newState()
        state.createProject(site("CED", password = "segreta"))
        assertTrue(state.hasPassword)
        assertTrue(state.project!!.isPasswordProtected)
        assertEquals("CED", state.project!!.businessUnits.single().areas.single().name)
    }

    @Test
    fun undoRestoresPreviousVersionsInOrder() {
        val state = newState()
        state.createProject(site("CED"))
        assertFalse(state.canUndo)

        val p0 = state.project!!
        state.update(ProjectEdits.addRack(p0, Rack(name = "R1")), "Rack R1 aggiunto")
        state.update(ProjectEdits.addRack(state.project!!, Rack(name = "R2")), "Rack R2 aggiunto")
        assertEquals(2, state.project!!.racks.size)
        assertEquals("Rack R2 aggiunto", state.undoLabel)

        state.undo()
        assertEquals(listOf("R1"), state.project!!.racks.map { it.name })
        state.undo()
        assertTrue(state.project!!.racks.isEmpty())
        assertFalse(state.canUndo)
    }

    @Test
    fun trashSurvivesReopeningTheProject() {
        val state = newState()
        state.createProject(site("CED"))
        val p = state.project!!
        val dev = Device(technicalName = "SW-01")
        state.update(ProjectEdits.addDevice(p, p.businessUnits.first().id, dev), "aggiunto")
        val (updated, item) = ProjectEdits.deleteDeviceToTrash(state.project!!, dev.id)
        state.addToTrash(item!!)
        state.update(updated, "eliminato")

        val file = state.storedProjects.first { it.id == p.id }.file
        state.closeProject()
        state.openStored(file)
        assertEquals(listOf("SW-01"), state.trash.map { it.displayName })
    }

    @Test
    fun mergeAppliesRemoteChangesAndAsksOnConflicts() {
        val state = newState()
        state.createProject(site("CED"))
        val p = state.project!!
        val buId = p.businessUnits.first().id
        val sw = Device(technicalName = "SW-01", ipAddress = "10.0.0.1")
        state.update(ProjectEdits.addDevice(p, buId, sw), "aggiunto")
        val exported = state.project!!
        state.storage.saveSyncBase(exported, null) // as after an export

        // Phone renames the device and adds a rack; the PC changes the same device's IP.
        val phone = ProjectEdits.addRack(ProjectEdits.updateDevice(exported, sw.copy(technicalName = "SW-PIANO1")), Rack(name = "R-Telefono"))
        state.update(ProjectEdits.updateDevice(state.project!!, sw.copy(ipAddress = "10.0.0.9")), "IP")

        state.startMerge(com.onlyfield.assetmanager.exchange.ProjectPackage(com.onlyfield.assetmanager.exchange.PackageManifest(exportId = "x", exportedEpochMs = 1L, projectId = p.id, projectName = p.name), phone))
        val merge = state.dialog as AppDialog.Merge
        assertEquals("SW-01", merge.current!!.name)
        state.chooseMergeSide(com.onlyfield.assetmanager.exchange.MergeSide.INCOMING)
        val merged = state.project!!
        assertEquals(listOf("R-Telefono"), merged.racks.map { it.name })
        assertEquals("SW-PIANO1", merged.businessUnits.first().devices.single().technicalName)
        assertTrue(state.canUndo)
    }
}
