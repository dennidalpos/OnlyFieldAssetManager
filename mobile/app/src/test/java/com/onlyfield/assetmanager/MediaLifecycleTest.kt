package com.onlyfield.assetmanager

import android.content.Context
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.UiMessage
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MediaLifecycleTest {
    @get:Rule val folder = TemporaryFolder()
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var root: File
    private lateinit var vm: ProjectViewModel
    private val owner = ViewModelStore()
    private val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messages = mutableListOf<UiMessage>()
    private val removed = Attachment(name = "Removed", originalFileName = "removed.bin", relativePath = "")
    private val active = Attachment(name = "Active", originalFileName = "active.bin", relativePath = "")
    private val project = Project(name = "Media", createdEpochMs = 1, updatedEpochMs = 1, attachments = listOf(removed, active))
    private val removedBytes = "recoverable bytes".toByteArray()
    private val activeBytes = "active bytes".toByteArray()

    @Before fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        root = folder.newFolder("attachments")
        repository = ProjectRepository(db, root)
        runBlocking { repository.saveProject(project) }
        write(removed, removedBytes)
        write(active, activeBytes)
        vm = ProjectViewModel(repository)
        owner.put("media", vm)
        collector.launch { vm.messages.collect { messages += it } }
        vm.openProject(project.id)
        await { vm.busy == null && vm.project.value != null }
    }

    @After fun tearDown() {
        collector.cancel()
        owner.clear()
        shadowOf(Looper.getMainLooper()).idle()
        db.close()
    }

    private fun file(att: Attachment, id: String = project.id) = AttachmentFiles.localFile(root, id, att)
    private fun write(att: Attachment, bytes: ByteArray) = file(att).apply { requireNotNull(parentFile).mkdirs(); writeBytes(bytes) }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        do {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(5)
        } while (System.nanoTime() < deadline)
        fail("Media lifecycle command did not finish")
    }

    @Test fun exportExcludesRemovedPayloadUndoKeepsHashAndLaterEditExpiresIt() {
        vm.edit("Remove") { ProjectEdits.deleteAttachment(it, removed.id) }
        await { vm.busy == null }
        assertTrue(file(removed).exists())
        runBlocking {
            PackageSerializer.importPackage(repository.exportProjectPackage(project.id)!!).pkg!!.use {
                assertEquals(setOf(AttachmentFiles.entryName(active)), it.attachments.keys)
                assertArrayEquals(activeBytes, AttachmentFiles.bytesIn(it, active))
            }
        }
        messages.first { it.text == "Remove" }.undo!!()
        await { vm.busy == null }
        assertEquals(project, vm.project.value)
        assertEquals(PackageSerializer.calculateSha256(removedBytes), PackageSerializer.calculateSha256(file(removed).readBytes()))
        vm.edit("Remove again") { ProjectEdits.deleteAttachment(it, removed.id) }
        await { vm.busy == null }
        vm.edit("Next revision") { it.copy(name = "Next") }
        await { vm.busy == null }
        assertFalse(file(removed).exists())
        assertArrayEquals(activeBytes, file(active).readBytes())
    }

    @Test fun closeExpiresUndoAndOpeningCollectsOversizedHistoricalOrphans() {
        vm.edit("Remove") { ProjectEdits.deleteAttachment(it, removed.id) }
        await { vm.busy == null }
        vm.closeProject()
        await { vm.busy == null }
        assertFalse(file(removed).exists())
        val orphan = Attachment(name = "Old oversized file", originalFileName = "orphan.bin", relativePath = "")
        val orphanFile = file(orphan).apply { requireNotNull(parentFile).mkdirs(); RandomAccessFile(this, "rw").use { it.setLength(33L * 1024 * 1024) } }
        vm.openProject(project.id)
        await { vm.busy == null }
        assertFalse(orphanFile.exists())
        assertArrayEquals(activeBytes, file(active).readBytes())
    }

    @Test fun permanentTrashDeletionDropsDeviceAndPortPhotosWhileRestoreKeepsThem() = runBlocking {
        val device = Device(technicalName = "Device")
        val port = Port(deviceId = device.id, name = "P1")
        val devicePhoto = removed.copy(targetType = AttachmentTargetType.DEVICE, targetId = device.id)
        val portPhoto = active.copy(targetType = AttachmentTargetType.PORT, targetId = port.id)
        val current = project.copy(sites = listOf(Site(name = "Site", devices = listOf(device.copy(ports = listOf(port))))),
            attachments = listOf(devicePhoto, portPhoto))
        for (emptyAll in listOf(false, true)) {
            repository.saveProject(current)
            write(devicePhoto, removedBytes); write(portPhoto, activeBytes)
            val item = repository.moveToTrash(project.id, "DEVICE", device.id)!!
            assertArrayEquals(removedBytes, file(devicePhoto).readBytes())
            PackageSerializer.importPackage(repository.exportProjectPackage(project.id)!!).pkg!!.use {
                assertEquals(repository.getProjectById(project.id)!!.updatedEpochMs, it.project.updatedEpochMs)
                assertTrue(it.project.attachments.isEmpty())
                assertTrue(it.attachments.isEmpty())
            }
            assertTrue(repository.restoreFromTrash(project.id, item.id))
            assertArrayEquals(activeBytes, file(portPhoto).readBytes())
            val deleted = repository.moveToTrash(project.id, "DEVICE", device.id)!!
            if (emptyAll) repository.emptyTrash(project.id) else repository.deleteTrashItemPermanently(deleted.id)
            assertTrue(repository.getProjectById(project.id)!!.attachments.isEmpty())
            assertTrue(repository.getTrashItems(project.id).isEmpty())
            assertFalse(file(devicePhoto).exists())
            assertFalse(file(portPhoto).exists())
        }
    }

    @Test fun replacementRetainsLocalTrashMediaMetadataWhileBaseAndExportStayIncoming() = runBlocking {
        val device = Device(technicalName = "Recoverable")
        val photo = removed.copy(targetType = AttachmentTargetType.DEVICE, targetId = device.id)
        val local = project.copy(sites = listOf(Site(name = "Site", devices = listOf(device))), attachments = listOf(photo))
        repository.saveProject(local)
        val item = repository.moveToTrash(project.id, "DEVICE", device.id)!!
        val incoming = project.copy(name = "Incoming", sites = local.sites.map { it.copy(devices = emptyList()) }, attachments = listOf(active))
        val evaluation = repository.evaluateImportPackage(java.io.ByteArrayInputStream(PackageSerializer.exportPackage(incoming,
            mapOf(AttachmentFiles.entryName(active) to activeBytes))))
        evaluation.importResult.pkg!!.use { repository.importProjectPackage(it) }
        assertEquals(incoming, repository.getSyncBase(project.id))
        assertEquals(setOf(active.id, photo.id), repository.getProjectById(project.id)!!.attachments.map { it.id }.toSet())
        assertEquals(PackageSerializer.calculateSha256(removedBytes), PackageSerializer.calculateSha256(file(photo).readBytes()))
        PackageSerializer.importPackage(repository.exportProjectPackage(project.id)!!).pkg!!.use {
            assertEquals(incoming.attachments, it.project.attachments)
            assertNull(AttachmentFiles.bytesIn(it, photo))
        }
        assertTrue(repository.restoreFromTrash(project.id, item.id))
        assertEquals(device, repository.getProjectById(project.id)!!.sites.flatMap { it.devices }.single())
        assertArrayEquals(removedBytes, file(photo).readBytes())
    }

    @Test fun unsupportedRestorePreservesSerializedAttachmentAndItsBytes() = runBlocking {
        val item = TrashItem(projectId = project.id, itemType = "ATTACHMENT", itemId = removed.id, displayName = removed.name,
            serializedJson = PackageSerializer.jsonConfig.encodeToString(Attachment.serializer(), removed))
        repository.saveProject(project.copy(attachments = listOf(active)))
        db.inventoryDao().insertTrashItems(listOf(com.onlyfield.assetmanager.data.repository.mappers.toTrashItemEntity(item)))
        assertTrue(runCatching { repository.restoreFromTrash(project.id, item.id) }.exceptionOrNull() is IllegalStateException)
        assertEquals(listOf(item), repository.getTrashItems(project.id))
        assertArrayEquals(removedBytes, file(removed).readBytes())
        assertFalse(repository.restoreFromTrash(project.id, "missing-item"))
    }

    @Test fun credentialRestorePreservesSecretAndCollisionKeepsBothVersions() = runBlocking {
        val credential = Credential(username = "Dummy", secret = "dummy-test-secret")
        val current = project.copy(credentials = listOf(credential))
        repository.saveProject(current)
        val item = repository.moveToTrash(project.id, "CREDENTIAL", credential.id)!!
        val collision = repository.getProjectById(project.id)!!.copy(credentials = listOf(credential.copy(secret = "other-dummy-test-secret")))
        repository.saveProject(collision)
        assertTrue(runCatching { repository.restoreFromTrash(project.id, item.id) }.exceptionOrNull() is IllegalStateException)
        assertEquals(collision, repository.getProjectById(project.id))
        assertEquals(listOf(item), repository.getTrashItems(project.id))
        repository.saveProject(collision.copy(credentials = emptyList()))
        assertTrue(repository.restoreFromTrash(project.id, item.id))
        assertEquals(credential, repository.getProjectById(project.id)!!.credentials.single())
        assertTrue(repository.getTrashItems(project.id).isEmpty())
    }

    @Test fun projectDeletionRemovesOnlyOwnedFilesAndRollsBackOnDatabaseFailure() = runBlocking {
        val otherAttachment = active.copy(id = java.util.UUID.randomUUID().toString())
        val other = project.copy(id = java.util.UUID.randomUUID().toString(), name = "Other", attachments = listOf(otherAttachment))
        repository.saveProject(other)
        val otherFile = file(otherAttachment, other.id).apply { requireNotNull(parentFile).mkdirs(); writeBytes(activeBytes) }
        val source = folder.newFile("user-source.bin").apply { writeBytes(removedBytes) }
        repository.exportProjectPackage(project.id)
        val base = repository.getSyncBase(project.id)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_delete BEFORE DELETE ON projects BEGIN SELECT RAISE(ABORT, 'injected deletion failure'); END")
        assertNotNull(runCatching { repository.deleteProject(project.id) }.exceptionOrNull())
        assertEquals(project, repository.getProjectById(project.id))
        assertEquals(base, repository.getSyncBase(project.id))
        assertArrayEquals(removedBytes, file(removed).readBytes())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_delete")
        repository.deleteProject(project.id)
        assertNull(repository.getProjectById(project.id))
        assertNull(repository.getSyncBase(project.id))
        assertFalse(File(root, project.id).exists())
        assertArrayEquals(activeBytes, otherFile.readBytes())
        assertArrayEquals(removedBytes, source.readBytes())
        assertNotNull(runCatching { repository.deleteProject("../outside") }.exceptionOrNull())
        assertArrayEquals(removedBytes, source.readBytes())
    }

    @Test fun failedPermanentTrashDeletionRestoresTheCatalogFilesAndTrash() = runBlocking {
        val device = Device(technicalName = "Recoverable")
        val photo = removed.copy(targetType = AttachmentTargetType.DEVICE, targetId = device.id)
        repository.saveProject(project.copy(sites = listOf(Site(name = "Site", devices = listOf(device))), attachments = listOf(photo)))
        val item = repository.moveToTrash(project.id, "DEVICE", device.id)!!
        val before = repository.getProjectById(project.id)!!
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_purge BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'injected purge failure'); END")
        assertNotNull(runCatching { repository.deleteTrashItemPermanently(item.id) }.exceptionOrNull())
        assertEquals(before, repository.getProjectById(project.id))
        assertEquals(listOf(item), repository.getTrashItems(project.id))
        assertArrayEquals(removedBytes, file(photo).readBytes())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_purge")
        repository.deleteTrashItemPermanently(item.id)
        assertTrue(repository.getProjectById(project.id)!!.attachments.isEmpty())
        assertFalse(file(photo).exists())
    }

    @Test fun missingSiteRejectsRestoreAndPreservesTrashMediaAndProject() = runBlocking {
        val device = Device(technicalName = "Recoverable")
        val photo = removed.copy(targetType = AttachmentTargetType.DEVICE, targetId = device.id)
        val local = project.copy(sites = listOf(Site(name = "Site", devices = listOf(device))), attachments = listOf(photo))
        repository.saveProject(local)
        val item = repository.moveToTrash(project.id, "DEVICE", device.id)!!
        val withoutSites = repository.getProjectById(project.id)!!.copy(sites = emptyList())
        repository.saveProject(withoutSites)
        assertTrue(runCatching { repository.restoreFromTrash(project.id, item.id) }.exceptionOrNull() is IllegalStateException)
        assertEquals(withoutSites, repository.getProjectById(project.id))
        assertEquals(listOf(item), repository.getTrashItems(project.id))
        assertArrayEquals(removedBytes, file(photo).readBytes())
        val otherSite = withoutSites.copy(sites = listOf(Site(name = "Other")))
        repository.saveProject(otherSite)
        assertTrue(runCatching { repository.restoreFromTrash(project.id, item.id) }.exceptionOrNull() is IllegalStateException)
        assertEquals(otherSite, repository.getProjectById(project.id))
        assertEquals(listOf(item), repository.getTrashItems(project.id))
        assertArrayEquals(removedBytes, file(photo).readBytes())
        repository.saveProject(otherSite.copy(sites = local.sites.map { it.copy(devices = emptyList()) }))
        assertTrue(repository.restoreFromTrash(project.id, item.id))
        assertEquals(device, repository.getProjectById(project.id)!!.sites.single().devices.single())
        assertArrayEquals(removedBytes, file(photo).readBytes())
    }

    @Test fun serializedAttachmentTrashKeepsItsPayloadUntilPermanentDeletion() = runBlocking {
        val item = TrashItem(projectId = project.id, itemType = "ATTACHMENT", itemId = removed.id, displayName = removed.name,
            serializedJson = PackageSerializer.jsonConfig.encodeToString(Attachment.serializer(), removed))
        repository.saveProject(project.copy(attachments = listOf(active)))
        db.inventoryDao().insertTrashItems(listOf(com.onlyfield.assetmanager.data.repository.mappers.toTrashItemEntity(item)))
        repository.collectMedia(project.id)
        assertArrayEquals(removedBytes, file(removed).readBytes())
        PackageSerializer.importPackage(repository.exportProjectPackage(project.id)!!).pkg!!.use {
            assertNull(AttachmentFiles.bytesIn(it, removed))
            assertArrayEquals(activeBytes, AttachmentFiles.bytesIn(it, active))
        }
        repository.deleteTrashItemPermanently(item.id)
        assertFalse(file(removed).exists())
        assertArrayEquals(activeBytes, file(active).readBytes())
    }
}
