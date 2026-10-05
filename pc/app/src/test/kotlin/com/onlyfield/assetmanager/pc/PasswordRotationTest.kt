package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.*
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PasswordRotationTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun changingProtectionPreservesAndReencryptsTheOriginalBase() {
        val dir = folder.newFolder()
        val storage = DesktopStorageManager(dir)
        val state = DesktopAppState(storage)
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Site", sites = listOf(Site(name = "BU", areas = listOf(Area(name = "Floor")))))))
            val base = state.project!!
            storage.saveSyncBase(base, null)
            state.update(base.copy(name = "Changed"), "Changed")
            for (password in listOf("one", "two", "")) {
                assertNull(state.changePassword(if (password == "one") "" else if (password == "two") "one" else "two", password, password))
                val bytes = File(dir, "sync/${base.id}.ofam").readBytes()
                if (password.isNotEmpty()) assertNull(PackageSerializer.importPackage(bytes).pkg)
                val restored = storage.loadSyncBase(base.id, password.ifEmpty { null })!!
                assertEquals(base.name, restored.name)
                assertEquals(base.sites, restored.sites)
            }
        } finally { state.shutdown() }
    }

    @Test fun unreadableBaseDoesNotChangeThePasswordOrProject() {
        val dir = folder.newFolder()
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Site", sites = listOf(Site(name = "BU", areas = listOf(Area(name = "Floor")))))))
            val before = state.project!!
            File(dir, "sync/${before.id}.ofam").apply { parentFile.mkdirs(); writeText("broken") }
            assertNotNull(state.changePassword("", "test", "test"))
            assertEquals(before, state.project)
            assertFalse(state.hasPassword)
        } finally { state.shutdown() }
    }

    @Test fun windowsWriteFailureRollsBackTheBaseAndPreservesTrash() {
        val dir = folder.newFolder()
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Site",
                sites = listOf(Site(name = "BU", areas = listOf(Area(name = "Floor")))))))
            val before = state.project!!
            val item = TrashItem(projectId = before.id, itemType = "DEVICE", itemId = "deleted",
                displayName = "Private item", serializedJson = "{}")
            state.trash = listOf(item)
            state.storage.saveSyncBase(before, null)
            val main = state.storedProjects.single().file
            val sync = File(dir, "sync/${before.id}.ofam")
            val mainBytes = main.readBytes()
            val syncBytes = sync.readBytes()
            // Windows sharing rules deny the real file replacement, after staging both packages.
            java.nio.file.Files.newByteChannel(main.toPath(), java.nio.file.StandardOpenOption.READ,
                com.sun.nio.file.ExtendedOpenOption.NOSHARE_DELETE).use {
                assertNotNull(state.changePassword("", "test", "test"))
                assertFalse(state.hasPassword)
                assertEquals(before, state.project)
                assertArrayEquals(mainBytes, main.readBytes())
                assertArrayEquals(syncBytes, sync.readBytes())
                state.trash = emptyList()
                assertEquals(listOf(item), state.trash)
                assertNotNull(state.error)
            }
            assertEquals(listOf(item), state.storage.loadTrash(before.id))
            assertFalse(dir.walkTopDown().any { it.extension == "tmp" })
        } finally { state.shutdown() }
    }

    @Test fun lostBaseRequiresExplicitReviewWithoutAutomaticChanges() {
        val dir = folder.newFolder()
        val state = DesktopAppState(DesktopStorageManager(dir))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Site",
                sites = listOf(Site(name = "BU", areas = listOf(Area(name = "Floor")))))))
            val before = state.project!!
            state.storage.saveSyncBase(before, null)
            assertTrue(File(dir, "sync/${before.id}.ofam").delete())
            val incoming = before.copy(name = "Remote change")
            val pkg = PackageSerializer.importPackage(PackageSerializer.exportPackage(incoming)).pkg!!
            state.startMerge(pkg)
            val merge = state.dialog as AppDialog.Merge
            assertEquals(0, merge.result.autoApplied)
            assertTrue(merge.result.conflicts.isNotEmpty())
            assertEquals(before, state.project)
            assertEquals(state.i18n.text("merge.baseUnavailable"), state.error)
        } finally { state.shutdown() }
    }
}
