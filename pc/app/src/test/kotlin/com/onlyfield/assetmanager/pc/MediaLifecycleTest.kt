package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import com.sun.nio.file.ExtendedOpenOption

class MediaLifecycleTest {
    @get:Rule val folder = TemporaryFolder()
    private val removed = Attachment(name = "Removed", originalFileName = "removed.bin", relativePath = "")
    private val active = Attachment(name = "Active", originalFileName = "active.bin", relativePath = "")
    private val project = Project(name = "Media", createdEpochMs = 1, updatedEpochMs = 1, attachments = listOf(removed, active))
    private val removedBytes = "recoverable bytes".toByteArray()
    private val activeBytes = "active bytes".toByteArray()
    private val media = mapOf(AttachmentFiles.entryName(removed) to removedBytes, AttachmentFiles.entryName(active) to activeBytes)

    @Test fun exportOmitsRemovedMediaAndLocalTrashWhileUndoRestoresTheSameHash() {
        for (password in listOf(null, "secret")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project, media, password)) }
            try {
                state.importFile(incoming, password)
                assertNull(state.error)
                val before = state.project!!
                state.update(ProjectEdits.deleteAttachment(before, removed.id), "Remove media")
                assertNull(state.error)
                val exported = folder.newFile()
                storage.exportPackageToFile(state.project!!, exported, password)
                storage.importPackageFromFile(exported, password).pkg!!.use { pkg ->
                    assertEquals(setOf(AttachmentFiles.entryName(active)), pkg.attachments.keys)
                    assertEquals(PackageSerializer.calculateSha256(activeBytes), PackageSerializer.calculateSha256(pkg.attachments.getValue(AttachmentFiles.entryName(active))))
                    assertNull(AttachmentFiles.bytesIn(pkg, removed))
                    assertFalse(pkg.attachments.containsKey(DesktopStorageManager.LOCAL_TRASH_ENTRY))
                }
                assertArrayEquals(removedBytes, storage.attachmentBytes(project.id, removed))
                state.undo()
                assertNull(state.error)
                assertEquals(before, state.project)
                assertEquals(PackageSerializer.calculateSha256(removedBytes), PackageSerializer.calculateSha256(state.attachmentBytes(removed)!!))
            } finally { state.shutdown() }
        }
    }

    @Test fun oversizedHistoricalOrphanDoesNotGetReadOrExportedAndItsSourceIsPreserved() {
        val storage = DesktopStorageManager(folder.newFolder())
        val owned = storage.attachmentFile(project.id, removed).apply {
            parentFile.mkdirs()
            RandomAccessFile(this, "rw").use { it.setLength(33L * 1024 * 1024) }
        }
        storage.storeAttachmentBytes(project.id, active, activeBytes)
        val exported = folder.newFile()
        storage.exportPackageToFile(project.copy(attachments = listOf(active)), exported)
        storage.importPackageFromFile(exported).pkg!!.use {
            assertEquals(setOf(AttachmentFiles.entryName(active)), it.attachments.keys)
            assertArrayEquals(activeBytes, AttachmentFiles.bytesIn(it, active))
        }
        assertEquals(33L * 1024 * 1024, owned.length())
    }

    @Test fun explicitPayloadSourceKeepsSupportedAliasesAndDropsUncataloguedPayloads() {
        val storage = DesktopStorageManager(folder.newFolder())
        val legacy = active.copy(relativePath = "legacy/photo.bin")
        val exported = folder.newFile()
        storage.exportPackageToFile(project.copy(attachments = listOf(legacy)), exported,
            attachments = mapOf("attachments/legacy/photo.bin" to activeBytes, "attachments/orphan.bin" to removedBytes,
                DesktopStorageManager.LOCAL_TRASH_ENTRY to "private trash".toByteArray()))
        storage.importPackageFromFile(exported).pkg!!.use {
            assertArrayEquals(activeBytes, AttachmentFiles.bytesIn(it, legacy))
            assertEquals(1, it.attachments.size)
        }
    }

    @Test fun closeExpiresUndoAndDurableCopyDropsRecoveryBytesWithAndWithoutProtection() {
        for (password in listOf(null, "secret")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project, media, password)) }
            try {
                state.importFile(incoming, password)
                state.update(ProjectEdits.deleteAttachment(state.project!!, removed.id), "Remove")
                assertNull(state.error)
                assertTrue(state.canUndo)
                val local = state.storedProjects.single().file
                state.closeProject()
                assertNull(state.project)
                assertFalse(state.canUndo)
                storage.importPackageFromFile(local, password).pkg!!.use {
                    assertNull(AttachmentFiles.bytesIn(it, removed))
                    assertArrayEquals(activeBytes, AttachmentFiles.bytesIn(it, active))
                }
                assertFalse(storage.attachmentFile(project.id, removed).exists())
                state.importFile(local, password, compare = false)
                assertNull(state.error)
                assertArrayEquals(activeBytes, state.attachmentBytes(active))
            } finally { state.shutdown() }
        }
    }

    @Test fun permanentTrashDeletionPurgesDeviceAndPortPhotosAndCannotBeUndone() {
        for (password in listOf(null, "secret")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val device = Device(technicalName = "Deleted")
            val port = Port(deviceId = device.id, name = "P1")
            val devicePhoto = removed.copy(targetType = AttachmentTargetType.DEVICE, targetId = device.id)
            val portPhoto = active.copy(targetType = AttachmentTargetType.PORT, targetId = port.id)
            val current = project.copy(sites = listOf(Site(name = "Site", devices = listOf(device.copy(ports = listOf(port))))),
                attachments = listOf(devicePhoto, portPhoto))
            val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(current, media, password)) }
            try {
                state.importFile(incoming, password)
                assertNull(state.error)
                val review = state.dialog as AppDialog.Compare
                assertTrue(review.warnings.isNotEmpty())
                state.acceptIncoming(review.pkg, review.password)
                assertNull(state.error)
                val (deleted, item) = ProjectEdits.deleteDeviceToTrash(state.project!!, device.id)
                state.addToTrash(item!!)
                state.update(deleted, "Trash")
                assertArrayEquals(removedBytes, state.attachmentBytes(devicePhoto))
                val exported = folder.newFile()
                storage.exportPackageToFile(state.project!!, exported, password)
                storage.importPackageFromFile(exported, password).pkg!!.use {
                    assertEquals(state.project!!.updatedEpochMs, it.project.updatedEpochMs)
                    assertTrue(it.project.attachments.isEmpty())
                    assertTrue(it.attachments.isEmpty())
                }
                state.restoreTrash(item)
                assertArrayEquals(activeBytes, state.attachmentBytes(portPhoto))
                state.undo()
                state.trash = emptyList()
                assertNull(state.error)
                assertTrue(state.project!!.attachments.isEmpty())
                assertFalse(state.canUndo)
                assertNull(storage.attachmentBytes(current.id, devicePhoto))
                assertNull(storage.attachmentBytes(current.id, portPhoto))
                val local = state.storedProjects.single().file
                storage.importPackageFromFile(local, password).pkg!!.use {
                    assertEquals(setOf(DesktopStorageManager.LOCAL_TRASH_ENTRY), it.attachments.keys)
                }
            } finally { state.shutdown() }
        }
    }

    @Test fun replacementPreservesClosedLocalTrashPhotosWithoutSharingThem() {
        for (password in listOf(null, "secret")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val device = Device(technicalName = "Recoverable")
            val photo = removed.copy(targetType = AttachmentTargetType.DEVICE, targetId = device.id)
            val local = project.copy(sites = listOf(Site(name = "Site", devices = listOf(device))), attachments = listOf(photo))
            val (deleted, item) = ProjectEdits.deleteDeviceToTrash(local, device.id)
            storage.storeAttachmentBytes(local.id, photo, removedBytes)
            storage.saveProjectLocally(deleted, password, trashItems = listOf(item!!))
            val incoming = project.copy(name = "Incoming", sites = local.sites.map { it.copy(devices = emptyList()) }, attachments = listOf(active))
            val packageFile = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(incoming,
                mapOf(AttachmentFiles.entryName(active) to activeBytes), password)) }
            try {
                state.importFile(packageFile, password)
                val review = state.dialog as AppDialog.Compare
                state.acceptIncoming(review.pkg, review.password)
                assertNull(state.error)
                assertEquals(incoming, storage.loadSyncBase(project.id, password))
                assertEquals(setOf(active.id, photo.id), state.project!!.attachments.map { it.id }.toSet())
                assertArrayEquals(removedBytes, state.attachmentBytes(photo))
                val exported = folder.newFile()
                storage.exportPackageToFile(state.project!!, exported, password)
                storage.importPackageFromFile(exported, password).pkg!!.use {
                    assertEquals(incoming.attachments, it.project.attachments)
                    assertNull(AttachmentFiles.bytesIn(it, photo))
                }
                state.restoreTrash(item)
                assertNull(state.error)
                assertArrayEquals(removedBytes, state.attachmentBytes(photo))
                assertEquals(device, state.project!!.sites.flatMap { it.devices }.single())
            } finally { state.shutdown() }
        }
    }

    @Test fun replacementWithSecondaryConflictsPreservesPlainAndProtectedCopies() {
        for (password in listOf(null, "dummy-password")) for (scenario in listOf("floor", "port", "container", "placement")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val area = Area(name = "Original floor")
            val rack = Rack(name = "Original rack", areaId = area.id)
            val draft = Device(technicalName = "Recoverable", areaId = area.id, rackId = rack.id, mountingType = MountingType.RACK_MOUNT)
            val port = Port(deviceId = draft.id, name = "P1")
            val device = draft.copy(ports = listOf(port))
            val other = Device(technicalName = "Active")
            val site = Site(name = "Same site", areas = listOf(area), devices = listOf(device))
            val placement = FloorplanPlacement(areaId = area.id, targetType = PlacementTargetType.DEVICE,
                targetId = device.id, xRatio = .2f, yRatio = .3f)
            val photo = removed.copy(targetType = AttachmentTargetType.PORT, targetId = port.id)
            val credential = Credential(username = "Dummy", secret = "dummy-test-secret")
            val original = project.copy(sites = listOf(site), racks = listOf(rack), attachments = listOf(photo, active),
                floorplanPlacements = listOf(placement), credentials = listOf(credential), isPasswordProtected = password != null)
            val (deleted, item) = ProjectEdits.deleteDeviceToTrash(original, device.id)
            storage.storeAttachmentBytes(project.id, photo, removedBytes)
            storage.storeAttachmentBytes(project.id, active, activeBytes)
            val localFile = storage.saveProjectLocally(deleted, password, trashItems = listOf(item!!))
            val incoming = original.copy(name = "Incoming $scenario", attachments = listOf(active),
                sites = listOf(site.copy(areas = if (scenario == "floor") emptyList() else listOf(area),
                    devices = listOf(if (scenario == "port") other.copy(ports = listOf(port.copy(deviceId = other.id))) else other))),
                racks = if (scenario in listOf("floor", "container")) emptyList() else listOf(rack),
                floorplanPlacements = if (scenario == "placement") listOf(placement.copy(targetId = other.id)) else emptyList())
            val packageFile = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(incoming,
                mapOf(AttachmentFiles.entryName(active) to activeBytes), password)) }
            try {
                state.importFile(packageFile, password)
                val review = state.dialog as AppDialog.Compare
                state.acceptIncoming(review.pkg, review.password)
                assertNull(state.error)
                val before = state.project!!
                val bytes = localFile.readBytes()
                state.restoreTrash(item)
                assertNotNull(state.error)
                assertEquals(before, state.project)
                assertEquals(listOf(item), state.trash)
                assertEquals(listOf(item), storage.loadTrash(project.id, password))
                assertEquals(incoming, storage.loadSyncBase(project.id, password))
                assertEquals(listOf(credential), state.project!!.credentials)
                assertArrayEquals(bytes, localFile.readBytes())
                assertArrayEquals(removedBytes, state.attachmentBytes(photo))
                assertArrayEquals(activeBytes, state.attachmentBytes(active))
                val exported = folder.newFile()
                storage.exportPackageToFile(before, exported, password)
                storage.importPackageFromFile(exported, password).pkg!!.use {
                    assertEquals(listOf(active), it.project.attachments)
                    assertEquals(setOf(AttachmentFiles.entryName(active)), it.attachments.keys)
                }
                state.update(before.copy(sites = listOf(site.copy(devices = listOf(other))), racks = listOf(rack), floorplanPlacements = emptyList()), "Repair context")
                state.restoreTrash(item)
                assertNull(state.error)
                assertEquals(device, state.project!!.sites.single().devices.first { it.id == device.id })
                assertEquals(listOf(placement), state.project!!.floorplanPlacements)
                assertTrue(state.trash.isEmpty())
                assertArrayEquals(removedBytes, state.attachmentBytes(photo))
            } finally { state.shutdown() }
        }
    }

    @Test fun historyLimitCollectsOnlyMediaWhoseLastUndoSnapshotExpires() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project, media)) }
        try {
            state.importFile(incoming)
            state.update(ProjectEdits.deleteAttachment(state.project!!, removed.id), "Remove")
            repeat(49) { state.update(state.project!!.copy(name = "Revision $it"), "Rename") }
            assertNull(state.error)
            assertArrayEquals(removedBytes, storage.attachmentBytes(project.id, removed))
            state.update(state.project!!.copy(name = "Expired"), "Rename")
            assertNull(state.error)
            assertFalse(storage.attachmentFile(project.id, removed).exists())
            assertNull(storage.attachmentBytes(project.id, removed))
            assertArrayEquals(activeBytes, state.attachmentBytes(active))
        } finally { state.shutdown() }
    }

    @Test fun missingSiteRejectsRestoreAndPreservesTrashMediaAndProject() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val device = Device(technicalName = "Recoverable")
        val photo = removed.copy(targetType = AttachmentTargetType.DEVICE, targetId = device.id)
        val local = project.copy(sites = listOf(Site(name = "Site", devices = listOf(device))), attachments = listOf(photo))
        val (deleted, item) = ProjectEdits.deleteDeviceToTrash(local, device.id)
        val noSites = deleted.copy(sites = emptyList())
        storage.storeAttachmentBytes(local.id, photo, removedBytes)
        val file = storage.saveProjectLocally(noSites, trashItems = listOf(item!!))
        try {
            state.openStored(file)
            assertNull(state.error)
            state.restoreTrash(item)
            assertNotNull(state.error)
            assertEquals(noSites, state.project)
            assertEquals(listOf(item), state.trash)
            assertArrayEquals(removedBytes, state.attachmentBytes(photo))
            assertEquals(listOf(item), storage.loadTrash(local.id))
            val otherSite = noSites.copy(sites = listOf(Site(name = "Other")))
            state.update(otherSite, "Other site")
            assertNull(state.error)
            state.restoreTrash(item)
            assertNotNull(state.error)
            assertEquals(otherSite, state.project)
            assertEquals(listOf(item), state.trash)
            assertArrayEquals(removedBytes, state.attachmentBytes(photo))
            state.update(otherSite.copy(sites = local.sites.map { it.copy(devices = emptyList()) }), "Original site")
            state.restoreTrash(item)
            assertNull(state.error)
            assertEquals(device, state.project!!.sites.single().devices.single())
            assertTrue(state.trash.isEmpty())
            assertArrayEquals(removedBytes, state.attachmentBytes(photo))
        } finally { state.shutdown() }
    }

    @Test fun lockedExpiredMediaRollsBackTheWorkingCopyCatalogAndCache() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project, media)) }
        try {
            state.importFile(incoming)
            val local = state.storedProjects.single().file
            val before = local.readBytes()
            val orphan = removed.copy(id = java.util.UUID.randomUUID().toString())
            storage.storeAttachmentBytes(project.id, orphan, removedBytes)
            val owned = storage.attachmentFile(project.id, orphan)
            Files.newByteChannel(owned.toPath(), setOf(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE)).use {
                state.update(state.project!!.copy(name = "Rejected"), "Rename")
            }
            assertNotNull(state.error)
            assertEquals(project, state.project)
            assertArrayEquals(before, local.readBytes())
            assertArrayEquals(removedBytes, owned.readBytes())
            assertArrayEquals(removedBytes, storage.attachmentBytes(project.id, orphan))
            state.update(state.project!!.copy(name = "Retry"), "Rename")
            assertNull(state.error)
            assertFalse(owned.exists())
            assertArrayEquals(activeBytes, state.attachmentBytes(active))
        } finally { state.shutdown() }
    }

    @Test fun unsupportedRestoreKeepsAttachmentMetadataAndProtectedBytes() {
        for (password in listOf(null, "dummy-password")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val item = TrashItem(projectId = project.id, itemType = "ATTACHMENT", itemId = removed.id, displayName = removed.name,
                serializedJson = PackageSerializer.jsonConfig.encodeToString(Attachment.serializer(), removed))
            storage.storeAttachmentBytes(project.id, removed, removedBytes)
            val current = project.copy(attachments = listOf(active), isPasswordProtected = password != null)
            val file = storage.saveProjectLocally(current, password, trashItems = listOf(item))
            try {
                state.importFile(file, password, compare = false)
                assertNull(state.error)
                val bytes = file.readBytes()
                state.restoreTrash(item)
                assertNotNull(state.error)
                assertEquals(current, state.project)
                assertEquals(listOf(item), state.trash)
                assertArrayEquals(bytes, file.readBytes())
                assertArrayEquals(removedBytes, state.attachmentBytes(removed))
                assertEquals(listOf(item), storage.loadTrash(project.id, password))
            } finally { state.shutdown() }
        }
    }

    @Test fun credentialRestorePreservesSecretAndCollisionKeepsBothVersions() {
        for (password in listOf(null, "dummy-password")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val state = DesktopAppState(storage)
            val credential = Credential(username = "Dummy", secret = "dummy-test-secret")
            val item = TrashItem(projectId = project.id, itemType = "CREDENTIAL", itemId = credential.id, displayName = "Dummy",
                serializedJson = PackageSerializer.jsonConfig.encodeToString(Credential.serializer(), credential))
            val collision = project.copy(isPasswordProtected = password != null,
                credentials = listOf(credential.copy(secret = "other-dummy-test-secret")))
            val file = storage.saveProjectLocally(collision, password, trashItems = listOf(item))
            try {
                state.importFile(file, password, compare = false)
                assertNull(state.error)
                val bytes = file.readBytes()
                state.restoreTrash(item)
                assertNotNull(state.error)
                assertEquals(collision, state.project)
                assertEquals(listOf(item), state.trash)
                assertArrayEquals(bytes, file.readBytes())
                state.update(collision.copy(credentials = emptyList()), "Remove collision")
                state.restoreTrash(item)
                assertNull(state.error)
                assertEquals(credential, state.project!!.credentials.single())
                assertTrue(state.trash.isEmpty())
                state.restoreTrash(item)
                assertNotNull(state.error)
                assertEquals(credential, state.project!!.credentials.single())
            } finally { state.shutdown() }
        }
    }
}
