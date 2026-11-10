package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.onboarding.*
import com.onlyfield.assetmanager.exchange.PackageSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProtectedTrashTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun protectedTrashSurvivesRestartAndRestoresTheDevice() {
        val dir = folder.newFolder()
        val state = DesktopAppState(DesktopStorageManager(dir))
        val reopened = DesktopAppState(DesktopStorageManager(dir))
        try {
            state.createProject(NewSiteWizard(draft = NewSiteDraft(projectName = "Site", sites = listOf(Site(name = "BU", areas = listOf(Area(name = "Floor")))), password = "test", passwordConfirm = "test")))
            val p = state.project!!
            val rack = Rack(name = "Rack", areaId = p.sites.single().areas.single().id)
            val dev = Device(technicalName = "Deleted private device", rackId = rack.id, areaId = rack.areaId, positionU = 3)
            state.update(ProjectEdits.addDevice(ProjectEdits.addRack(p, rack), p.sites.single().id, dev), "added")
            val storedDevice = state.project!!.sites.single().devices.single()
            val (updated, item) = ProjectEdits.deleteDeviceToTrash(state.project!!, dev.id)
            state.addToTrash(item!!)
            state.update(updated, "deleted")
            val file = state.storedProjects.single().file
            state.shutdown()
            reopened.importFile(file, "test", compare = false)
            assertNull(reopened.error)
            assertEquals(item, reopened.trash.single())
            assertEquals(storedDevice, ProjectEdits.restoreFromTrash(reopened.project!!, reopened.trash.single()).sites.single().devices.single())
            assertFalse(File(dir, "trash/${p.id}.json").exists())
            val encrypted = PackageSerializer.importPackage(file.readBytes(), "test").pkg!!
            assertFalse(file.readBytes().toString(Charsets.UTF_8).contains(dev.technicalName))
            val exported = File(dir, "export.ofam")
            reopened.storage.exportPackageToFile(reopened.project!!, exported, "test")
            assertFalse(PackageSerializer.importPackage(exported.readBytes(), "test").pkg!!.attachments.containsKey(DesktopStorageManager.LOCAL_TRASH_ENTRY))
            reopened.restoreTrash(item)
            assertTrue(reopened.trash.isEmpty())
            reopened.undo()
            assertEquals(item, reopened.trash.single())
            reopened.trash = emptyList()
            reopened.closeProject()
            reopened.importFile(file, "test", compare = false)
            assertTrue(reopened.trash.isEmpty())
            assertTrue(encrypted.attachments.containsKey(DesktopStorageManager.LOCAL_TRASH_ENTRY))
        } finally { state.shutdown(); reopened.shutdown() }
    }

    @Test fun legacyTrashMigratesAndCorruptionNeverOverwritesTheCopy() {
        val dir = folder.newFolder()
        val storage = DesktopStorageManager(dir)
        val project = NewSiteWizard(draft = NewSiteDraft(projectName = "Site", sites = listOf(Site(name = "BU", areas = listOf(Area(name = "Floor")))))).buildProject()
        val local = File(storage.getProjectsFolder(), "${project.id}.ofam")
        local.writeBytes(PackageSerializer.exportPackage(project))
        val item = TrashItem(projectId = project.id, itemType = "DEVICE", itemId = "deleted", displayName = "Legacy", serializedJson = "{}")
        val legacy = File(dir, "trash/${project.id}.json").apply { parentFile.mkdirs() }
        legacy.writeText(PackageSerializer.jsonConfig.encodeToString(ListSerializer(TrashItem.serializer()), listOf(item)))
        assertEquals(listOf(item), storage.loadTrash(project.id))
        storage.saveProjectLocally(project)
        assertFalse(legacy.exists())
        assertEquals(listOf(item), storage.loadTrash(project.id))
        local.writeBytes(PackageSerializer.exportPackage(project, mapOf(DesktopStorageManager.LOCAL_TRASH_ENTRY to "broken".toByteArray())))
        val before = local.readBytes()
        val state = DesktopAppState(storage)
        try {
            state.openStored(local)
            assertNull(state.project)
            assertNotNull(state.error)
            assertArrayEquals(before, local.readBytes())
        } finally { state.shutdown() }
    }
}
