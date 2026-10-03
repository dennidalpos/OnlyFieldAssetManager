package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.onboarding.NewSiteDraft
import com.onlyfield.assetmanager.core.onboarding.NewSiteWizard
import com.onlyfield.assetmanager.core.model.BusinessUnit
import com.onlyfield.assetmanager.core.model.Area
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WorkingCopyLockTest {
    @get:Rule val folder = TemporaryFolder()

    private fun wizard(password: String = "") = NewSiteWizard(draft = NewSiteDraft(
        projectName = "Lock test", businessUnits = listOf(BusinessUnit(name = "BU", areas = listOf(Area(name = "Floor")))),
        password = password, passwordConfirm = password))

    @Test fun uiOpeningCannotStealAnotherWorkingCopy() {
        val dir = folder.newFolder()
        val first = DesktopAppState(DesktopStorageManager(dir))
        val second = DesktopAppState(DesktopStorageManager(dir))
        try {
            first.createProject(wizard())
            val file = first.storedProjects.single().file
            try {
                second.storage.saveProjectLocally(first.project!!)
                fail("Another manager must not overwrite the locked copy")
            } catch (_: IllegalStateException) { }
            second.createProject(wizard())
            val previous = second.project
            second.openStored(file)
            assertEquals(previous, second.project)
            assertNotNull(second.error)
            first.closeProject()
            second.openStored(file)
            assertEquals(file.nameWithoutExtension, second.project!!.id)
            assertNull(second.error)
        } finally { first.shutdown(); second.shutdown() }
    }

    @Test fun staleMarkerAndFailedPasswordDoNotHoldALock() {
        val dir = folder.newFolder()
        val storage = DesktopStorageManager(dir)
        val state = DesktopAppState(storage)
        val protected = wizard("test")
        state.createProject(protected)
        val id = state.project!!.id
        state.shutdown()
        val reader = DesktopStorageManager(dir)
        val other = DesktopStorageManager(dir)
        try {
            assertNull(reader.loadLocalProject(id, "wrong").pkg)
            other.acquireProjectLock(id)
        } finally { reader.releaseAllLocks(); other.releaseAllLocks() }
    }

    @Test fun failedOpeningPreservesAPreviouslyOwnedLock() {
        val storage = DesktopStorageManager(folder.newFolder())
        val project = wizard().buildProject()
        val file = java.io.File(storage.getProjectsFolder(), "${project.id}.ofam")
        file.writeBytes(com.onlyfield.assetmanager.exchange.PackageSerializer.exportPackage(project,
            mapOf(DesktopStorageManager.LOCAL_TRASH_ENTRY to "broken".toByteArray())))
        storage.acquireProjectLock(project.id)
        val state = DesktopAppState(storage)
        try {
            state.openStored(file)
            assertNull(state.project)
            assertNotNull(state.error)
            assertTrue(storage.ownsProjectLock(project.id))
        } finally { state.shutdown() }
    }
}
