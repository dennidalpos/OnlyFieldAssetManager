package com.onlyfield.assetmanager

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.cartography.MapSnapshotRequest
import com.onlyfield.assetmanager.cartography.MapSnapshotResult
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.ui.ProjectViewModel
import com.onlyfield.assetmanager.ui.UiMessage
import com.onlyfield.assetmanager.ui.screens.EditSave
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
class SpecializedCommandTest {
    @get:Rule val folder = TemporaryFolder()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var vm: ProjectViewModel
    private lateinit var root: File
    private lateinit var source: File
    private lateinit var context: Context
    private val owner = ViewModelStore()
    private val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messages = mutableListOf<UiMessage>()
    private val device = Device(technicalName = "Survivor")
    private val duplicate = Device(technicalName = "Duplicate")
    private val initial = Project(name = "Isolated commands", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(Site(name = "Site", devices = listOf(device, duplicate))))

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        root = folder.newFolder("attachments")
        source = folder.newFile("source.bin").apply { writeText("Synthetic source") }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
            .setTransactionExecutor(executor).build()
        repository = ProjectRepository(db, root, "dummy-special-recovery")
        runBlocking { repository.saveProject(initial) }
        // Native tests cover PNG bytes; this source isolates command publication.
        vm = ProjectViewModel(repository) { _, _ ->
            MapSnapshotResult(byteArrayOf(1, 2, 3), widthPx = 1, heightPx = 1, attributionText = "Synthetic attribution", sourceName = "Test source")
        }
        owner.put("special-commands", vm)
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
        fail("Specialized command did not finish")
    }

    private fun holdTransactions(): CountDownLatch {
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        executor.execute { reached.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        return release
    }

    private fun submit(kind: String, result: (String?) -> Unit) {
        when (kind) {
            "replace" -> vm.replaceDevice(device.id, "Replacement", DeviceCategory.NETWORK_SWITCH, result)
            "merge" -> vm.mergeDevices(device.id, duplicate.id, MergeDataChoices(), result)
            "attachment" -> vm.addAttachment(context, Uri.fromFile(source), "Imported", AttachmentClassification.SHAREABLE, result)
            "download" -> vm.downloadMap(MapSnapshotRequest(), "Downloaded", result)
            else -> error("Unknown command: $kind")
        }
    }

    @Test fun replacementReportsFailureThenOneRetry() = verifyRetry("replace")
    @Test fun mergeReportsFailureThenOneRetry() = verifyRetry("merge")
    @Test fun attachmentReportsFailureAfterCleanupThenOneRetry() = verifyRetry("attachment")
    @Test fun downloadReportsFailureAfterCleanupThenOneRetry() = verifyRetry("download")

    private fun verifyRetry(kind: String) {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_special BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'special save denied'); END")
        val outcomes = mutableListOf<String?>()
        submit(kind) { result ->
            outcomes += result
            if (result != null) assertTrue(AttachmentFiles.ownedFiles(root, initial.id).isEmpty())
        }
        await { vm.busy == null }
        assertEquals(1, outcomes.size)
        assertTrue(outcomes.single()!!.contains("special save denied"))
        assertEquals(initial, vm.project.value)
        assertTrue(vm.trash.value.isEmpty())
        assertTrue(messages.none { it.undo != null })
        runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_special")
        submit(kind) { outcomes += it }
        await { vm.busy == null }
        assertEquals(2, outcomes.size)
        assertNull(outcomes.last())
        val saved = vm.project.value!!
        if (kind == "replace" || kind == "merge") {
            assertEquals(if (kind == "replace") 2 else 1, saved.sites.single().devices.size)
            assertEquals(1, vm.trash.value.size)
            assertTrue(messages.none { it.undo != null })
        } else {
            assertEquals(1, saved.attachments.size)
            assertEquals(1, AttachmentFiles.ownedFiles(root, initial.id).size)
            messages.single { it.undo != null }.undo!!.invoke()
            await { vm.busy == null }
            assertEquals(initial, vm.project.value)
            assertTrue(AttachmentFiles.ownedFiles(root, initial.id).isEmpty())
        }
        runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
    }

    @Test fun closedSessionSuppressesReplacementOutcome() = verifyLate("replace", "session")
    @Test fun closedSessionSuppressesMergeOutcome() = verifyLate("merge", "session")
    @Test fun closedSessionSuppressesAttachmentOutcome() = verifyLate("attachment", "session")
    @Test fun closedSessionSuppressesDownloadOutcome() = verifyLate("download", "session")
    @Test fun cancellationSuppressesReplacementOutcome() = verifyLate("replace", "cancel")
    @Test fun cancellationSuppressesMergeOutcome() = verifyLate("merge", "cancel")
    @Test fun cancellationSuppressesAttachmentOutcome() = verifyLate("attachment", "cancel")
    @Test fun cancellationSuppressesDownloadOutcome() = verifyLate("download", "cancel")
    @Test fun disposedEditorIgnoresReplacementOutcome() = verifyLate("replace", "editor")
    @Test fun disposedEditorIgnoresMergeOutcome() = verifyLate("merge", "editor")
    @Test fun disposedEditorIgnoresAttachmentOutcome() = verifyLate("attachment", "editor")
    @Test fun disposedEditorIgnoresDownloadOutcome() = verifyLate("download", "editor")

    private fun verifyLate(kind: String, mode: String) {
        if (mode == "session") db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_late BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'late special save denied'); END")
        val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val save = EditSave(vm, uiScope)
        var closes = 0
        var callbacks = 0
        val release = holdTransactions()
        try {
            save.submit({ closes++ }) { onResult -> submit(kind) { callbacks++; onResult(it) } }
            shadowOf(Looper.getMainLooper()).idle()
            when (mode) {
                "session" -> vm.closeProject()
                "cancel" -> owner.clear()
                "editor" -> uiScope.cancel()
            }
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, closes)
        assertNull(save.error)
        if (mode != "editor") {
            assertEquals(0, callbacks)
            assertTrue(messages.isEmpty())
            runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
            assertTrue(AttachmentFiles.ownedFiles(root, initial.id).isEmpty())
        } else {
            assertEquals(1, callbacks)
            assertNotEquals(initial, vm.project.value)
        }
        if (mode == "session") assertNull(vm.project.value)
        uiScope.cancel()
    }

    @Test fun invalidMergeReportsRejectionWithoutSuccessOrTrash() {
        val outcomes = mutableListOf<String?>()
        vm.mergeDevices(device.id, device.id, MergeDataChoices()) { outcomes += it }
        await { vm.busy == null }
        assertEquals(1, outcomes.size)
        assertNotNull(outcomes.single())
        assertEquals(initial, vm.project.value)
        assertTrue(vm.trash.value.isEmpty())
        assertEquals(1, messages.size)
        assertTrue(messages.single().isError)
    }

    @Test fun unreadableAttachmentReportsFailureAndSameUriCanBeRetried() {
        check(source.delete())
        val outcomes = mutableListOf<String?>()
        submit("attachment") { outcomes += it }
        await { vm.busy == null }
        assertEquals(1, outcomes.size)
        assertNotNull(outcomes.single())
        assertEquals(initial, vm.project.value)
        assertTrue(AttachmentFiles.ownedFiles(root, initial.id).isEmpty())
        source.writeText("Synthetic retry")
        submit("attachment") { outcomes += it }
        await { vm.busy == null }
        assertEquals(2, outcomes.size)
        assertNull(outcomes.last())
        val added = vm.project.value!!.attachments.single()
        assertArrayEquals(source.readBytes(), AttachmentFiles.localFile(root, initial.id, added).readBytes())
    }
}
