package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.pc.ui.components.DetailSlot
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class DetailChangeTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun pendingExitCanBeCancelledAndDiscardRunsOnce() {
        val slot = DetailSlot()
        var closes = 0
        var exits = 0
        slot.dismiss = { closes++ }
        slot.dirty = true
        slot.requestChange { exits++ }
        assertEquals(0, exits)
        slot.pendingChange = null
        assertTrue(slot.dirty)
        slot.requestChange { exits++ }
        slot.requestChange { exits += 10 }
        slot.change(slot.pendingChange!!)
        assertEquals(1, exits)
        assertEquals(1, closes)
        assertNull(slot.pendingChange)
        slot.requestChange {}
        assertEquals(1, closes)
    }

    @Test fun globalActionsWaitForTheActiveDraft() {
        val state = DesktopAppState(DesktopStorageManager(folder.newFolder()))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Sito", sites = listOf(
                com.onlyfield.assetmanager.core.model.Site(name = "BU", areas = listOf(com.onlyfield.assetmanager.core.model.Area(name = "Piano")))
            ))))
            val original = state.project!!
            state.update(ProjectEdits.addRack(original, Rack(name = "R1")), "Rack")
            state.detailSlot.dirty = true
            state.section = AppSection.INVENTORY
            assertEquals(AppSection.FLOORPLANS, state.section)
            state.detailSlot.pendingChange = null
            state.section = AppSection.FLOORPLANS
            assertNull(state.detailSlot.pendingChange)
            state.undo()
            assertEquals(1, state.project!!.racks.size)
            state.detailSlot.pendingChange = null
            state.newProject()
            assertNull(state.dialog)
            state.detailSlot.pendingChange = null
            state.closeProject()
            assertNotNull(state.project)
            state.detailSlot.change(state.detailSlot.pendingChange!!)
            assertNull(state.project)
        } finally { state.shutdown() }
    }
}
