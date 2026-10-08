package com.onlyfield.assetmanager

import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.Screen
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class DeletionCommandTest {
    @get:Rule val folder = TemporaryFolder()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var vm: ProjectViewModel
    private lateinit var root: File
    private val owner = ViewModelStore()
    private val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messages = mutableListOf<UiMessage>()
    private val rack = Rack(name = "Synthetic rack", heightU = 12)
    private val device = Device(technicalName = "Synthetic device", rackId = rack.id, positionU = 1, mountingType = MountingType.RACK_MOUNT)
    private val attachments = listOf(
        Attachment(name = "Device document", originalFileName = "device.txt", relativePath = "", fileType = AttachmentType.DOCUMENT,
            mimeType = "text/plain", targetType = AttachmentTargetType.DEVICE, targetId = device.id),
        Attachment(name = "Rack document", originalFileName = "rack.txt", relativePath = "", fileType = AttachmentType.DOCUMENT,
            mimeType = "text/plain", targetType = AttachmentTargetType.RACK, targetId = rack.id))
    private val initial = ObjectHierarchy.synchronize(Project(name = "Isolated deletion", createdEpochMs = 1, updatedEpochMs = 1,
        racks = listOf(rack), sites = listOf(Site(name = "Synthetic site", devices = listOf(device))), attachments = attachments))

    @Before fun setUp() {
        root = folder.newFolder("media")
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().setTransactionExecutor(executor).build()
        repository = ProjectRepository(db, root, "isolated-deletion-recovery")
        runBlocking { repository.saveProject(initial) }
        attachments.forEach { attachment ->
            AttachmentFiles.localFile(root, initial.id, attachment).apply {
                check(parentFile!!.mkdirs() || parentFile!!.isDirectory)
                writeText(attachment.name)
            }
        }
        vm = ProjectViewModel(repository)
        owner.put("deletion", vm)
        collector.launch { vm.messages.collect { messages += it } }
        vm.openProject(initial.id)
        await { vm.project.value != null && vm.busy == null }
    }

    @After fun tearDown() {
        collector.cancel()
        owner.clear()
        shadowOf(Looper.getMainLooper()).idle()
        db.close()
        executor.shutdownNow()
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        do {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(5)
        } while (System.nanoTime() < deadline)
        fail("Deletion did not finish")
    }

    private fun holdTransactions(): CountDownLatch {
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        executor.execute { reached.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        return release
    }

    private fun submit(kind: String) = vm.moveToTrash(kind, if (kind == "DEVICE") device.id else rack.id, "Synthetic object")
    private fun denyDelete() = db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_delete BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated deletion denied'); END")
    private fun assertMedia() = attachments.forEach { attachment ->
        assertEquals(attachment.name, AttachmentFiles.localFile(root, initial.id, attachment).readText())
    }

    @Test fun failedDeviceDeletionRetriesOnceAndUndoRestoresMedia() = verifyRetry("DEVICE")
    @Test fun failedRackDeletionRetriesOnceAndUndoRestoresPlacement() = verifyRetry("RACK")

    private fun verifyRetry(kind: String) {
        denyDelete()
        submit(kind)
        await { vm.busy == null }
        assertEquals(initial, vm.project.value)
        assertTrue(vm.trash.value.isEmpty())
        assertTrue(messages.single().isError)
        assertTrue(messages.single().text.contains("isolated deletion denied"))
        assertNull(messages.single().undo)
        runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
        assertMedia()
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_delete")
        submit(kind)
        submit(kind)
        await { vm.busy == null }
        assertEquals(1, vm.trash.value.size)
        assertEquals(1, messages.count { it.undo != null })
        val saved = vm.project.value!!
        if (kind == "DEVICE") assertTrue(saved.sites.single().devices.isEmpty())
        else { assertTrue(saved.racks.isEmpty()); assertNull(saved.sites.single().devices.single().rackId) }
        assertMedia()
        messages.single { it.undo != null }.undo!!.invoke()
        await { vm.busy == null }
        assertTrue(vm.trash.value.isEmpty())
        assertEquals(initial.racks, vm.project.value!!.racks)
        assertEquals(initial.sites, vm.project.value!!.sites)
        assertEquals(initial.attachments, vm.project.value!!.attachments)
        runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
        assertMedia()
    }

    @Test fun missingDeviceDoesNotPublishSuccess() = verifyMissing("DEVICE")
    @Test fun missingRackDoesNotPublishSuccess() = verifyMissing("RACK")
    private fun verifyMissing(kind: String) {
        vm.moveToTrash(kind, "absent-id", "Missing object")
        await { vm.busy == null }
        assertEquals(initial, vm.project.value)
        assertTrue(vm.trash.value.isEmpty())
        assertTrue(messages.isEmpty())
        assertMedia()
    }

    @Test fun closedSessionSuppressesDeviceError() = verifyLate("DEVICE", "close")
    @Test fun switchedSessionSuppressesRackError() = verifyLate("RACK", "switch-error")
    @Test fun successfulDeviceDeletionCannotNavigateAnotherScreen() = verifyLate("DEVICE", "navigate")
    @Test fun successfulRackDeletionCannotPublishIntoAnotherProject() = verifyLate("RACK", "switch-success")
    @Test fun cancelledDeviceDeletionCannotPublish() = verifyLate("DEVICE", "cancel")
    @Test fun cancelledRackDeletionCannotPublish() = verifyLate("RACK", "cancel")

    private fun verifyLate(kind: String, mode: String) {
        val other = Project(name = "Other isolated project", createdEpochMs = 1, updatedEpochMs = 1)
        runBlocking { repository.saveProject(other) }
        if (mode == "close" || mode == "switch-error") denyDelete()
        val release = holdTransactions()
        try {
            submit(kind)
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(vm.busy)
            assertEquals(initial, vm.project.value)
            when (mode) {
                "close" -> vm.closeProject()
                "switch-error", "switch-success" -> { vm.closeProject(); vm.openProject(other.id) }
                "navigate" -> vm.navigate(Screen.Attachments)
                "cancel" -> owner.clear()
            }
        } finally { release.countDown() }
        await { vm.busy == null }
        if (mode == "navigate") {
            assertEquals(Screen.Attachments, vm.currentScreen)
            assertNotEquals(initial, vm.project.value)
            assertEquals(1, vm.trash.value.size)
        } else {
            assertTrue(messages.isEmpty())
            assertTrue(vm.trash.value.isEmpty())
            if (mode == "close") assertNull(vm.project.value)
            if (mode.startsWith("switch")) assertEquals(other, vm.project.value)
            runBlocking {
                val persisted = repository.getProjectById(initial.id)!!
                if (mode == "switch-success") assertTrue(persisted.racks.isEmpty())
                else assertEquals(initial, persisted)
            }
        }
        assertMedia()
    }
}
