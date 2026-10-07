package com.onlyfield.assetmanager

import android.os.Looper
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ProjectCommandTest {
    @get:Rule val folder = TemporaryFolder()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var vm: ProjectViewModel
    private val owner = ViewModelStore()
    private val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messages = mutableListOf<UiMessage>()
    private val device = Device(technicalName = "Switch")
    private val initial = Project(name = "Initial", description = "", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(Site(name = "Site", devices = listOf(device))))

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().setTransactionExecutor(executor).build()
        repository = ProjectRepository(db)
        runBlocking { repository.saveProject(initial) }
        vm = ProjectViewModel(repository)
        owner.put("commands", vm)
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
        fail("Command did not finish")
    }

    private fun holdTransactions(): CountDownLatch {
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        executor.execute { reached.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        return release
    }

    @Test fun rapidEditsUseCommittedStateAndFailedFirstSaveCannotEraseTheSecond() {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_first BEFORE UPDATE ON projects WHEN NEW.name = 'Fail' BEGIN SELECT RAISE(ABORT, 'injected first save failure'); END")
        val release = holdTransactions()
        try {
            vm.edit("First") { it.copy(name = "Fail") }
            vm.edit("Second") { it.copy(description = "Second survives") }
            shadowOf(Looper.getMainLooper()).idle()
            assertNotNull(vm.busy)
            assertEquals(initial, vm.project.value)
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals("Initial", vm.project.value!!.name)
        assertEquals("Second survives", vm.project.value!!.description)
        assertTrue(messages.any { it.isError })
        runBlocking { assertEquals(vm.project.value, repository.getProjectById(initial.id)) }
        vm.edit("Third") { it.copy(name = "Third") }
        vm.edit("Fourth") { it.copy(description = it.description.orEmpty() + "; Fourth") }
        await { vm.busy == null }
        assertEquals("Third", vm.project.value!!.name)
        assertEquals("Second survives; Fourth", vm.project.value!!.description)
    }

    @Test fun editsTrashAndPasswordStayOrderedAndOldUndoCannotRestoreHiddenState() {
        vm.edit("Rename") { it.copy(name = "Renamed") }
        vm.moveToTrash("DEVICE", device.id, device.technicalName)
        var passwordResult: String? = "pending"
        vm.changePassword("", "new-password") { passwordResult = it }
        vm.edit("Notes") { it.copy(description = "Final") }
        await { vm.busy == null }
        assertNull(passwordResult)
        val final = vm.project.value!!
        assertEquals("Renamed", final.name)
        assertEquals("Final", final.description)
        assertTrue(final.isPasswordProtected)
        assertTrue(final.sites.single().devices.isEmpty())
        assertEquals(1, vm.trash.value.size)
        messages.first { it.text == "Rename" }.undo!!()
        await { vm.busy == null }
        assertEquals(final, vm.project.value)
        runBlocking { assertTrue(repository.verifyProjectPassword(initial.id, "new-password")) }
        messages.first { it.text == "Notes" }.undo!!()
        await { vm.busy == null }
        assertEquals("", vm.project.value!!.description)
        assertTrue(vm.project.value!!.isPasswordProtected)
        assertEquals(1, vm.trash.value.size)
    }

    @Test fun closeOrSwitchDuringSaveNeverReopensTheOldProjectAndQueuedEditsStillPersist() {
        val other = initial.copy(id = java.util.UUID.randomUUID().toString(), name = "Other", sites = emptyList())
        runBlocking { repository.saveProject(other) }
        val release = holdTransactions()
        try {
            vm.edit("Old edit") { it.copy(description = "Kept after close") }
            shadowOf(Looper.getMainLooper()).idle()
            vm.closeProject()
            vm.openProject(other.id)
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(other, vm.project.value)
        runBlocking { assertEquals("Kept after close", repository.getProjectById(initial.id)!!.description) }
        vm.edit("Other edit") { it.copy(description = "Kept") }
        await { vm.busy == null }
        val undo = messages.first { it.text == "Other edit" }.undo!!
        vm.closeProject()
        undo()
        await { vm.busy == null }
        assertNull(vm.project.value)
        runBlocking { assertEquals("Kept", repository.getProjectById(other.id)!!.description) }
    }

    @Test fun queuedExportChecksThePasswordAfterPreviousCommandsCommit() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        val rejected = java.io.File(folder.root, "rejected.ofam")
        vm.changePassword("", "new-password") {}
        vm.edit("New description") { it.copy(description = "Exported version") }
        vm.exportPackage(context.contentResolver, Uri.fromFile(rejected), null)
        await { vm.busy == null }
        assertFalse(rejected.exists())
        assertTrue(messages.any { it.isError })
        val accepted = java.io.File(folder.root, "accepted.ofam")
        vm.exportPackage(context.contentResolver, Uri.fromFile(accepted), "new-password")
        await { vm.busy == null }
        accepted.inputStream().use { com.onlyfield.assetmanager.exchange.PackageSerializer.importPackage(it, "new-password") }.pkg!!.use {
            assertEquals(vm.project.value, it.project)
            assertEquals("Exported version", it.project.description)
            assertTrue(it.manifest.isEncrypted)
        }
    }

    @Test fun passwordDatabaseFailureReturnsOneErrorAndKeepsProtectionUnchanged() {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_password BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'password write denied'); END")
        val results = mutableListOf<String?>()
        vm.changePassword("", "new-password") { results += it }
        await { vm.busy == null }
        assertEquals(1, results.size)
        assertNotNull(results.single())
        assertEquals(initial, vm.project.value)
        runBlocking { assertFalse(repository.getProjectById(initial.id)!!.isPasswordProtected) }
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_password")
        vm.changePassword("", "new-password") { results += it }
        await { vm.busy == null }
        assertNull(results.last())
        assertTrue(vm.project.value!!.isPasswordProtected)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_password BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'password removal denied'); END")
        vm.changePassword("new-password", "") { results += it }
        await { vm.busy == null }
        assertEquals(3, results.size)
        assertNotNull(results.last())
        assertTrue(vm.project.value!!.isPasswordProtected)
        runBlocking { assertTrue(repository.verifyProjectPassword(initial.id, "new-password")) }
    }

    @Test fun malformedProjectDoesNotReplaceTheOpenProjectOrPreventRetry() {
        db.openHelper.writableDatabase.execSQL("UPDATE projects SET objectTypesJson = 'broken'")
        vm.openProject(initial.id)
        await { vm.busy == null }
        assertEquals(initial, vm.project.value)
        assertTrue(messages.any { it.isError })
        db.openHelper.writableDatabase.execSQL("UPDATE projects SET objectTypesJson = '[]'")
        vm.openProject(initial.id)
        await { vm.busy == null }
        assertEquals(initial, vm.project.value)
    }

    @Test fun failedTrashCommandsPreserveItemsAndPublishOnlyErrors() {
        vm.moveToTrash("DEVICE", device.id, device.technicalName)
        await { vm.busy == null }
        val item = vm.trash.value.single()
        val before = vm.project.value
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_trash BEFORE DELETE ON trash_items BEGIN SELECT RAISE(ABORT, 'trash delete denied'); END")
        messages.clear()
        vm.restoreFromTrash(item.id)
        vm.deleteFromTrash(item.id)
        vm.emptyTrash()
        await { vm.busy == null }
        assertEquals(3, messages.size)
        assertTrue(messages.all { it.isError })
        assertEquals(before, vm.project.value)
        assertEquals(listOf(item), vm.trash.value)
        runBlocking { assertEquals(before, repository.getProjectById(initial.id)) }
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_trash")
        vm.restoreFromTrash(item.id)
        await { vm.busy == null }
        assertEquals(device, vm.project.value!!.sites.single().devices.single())
        assertTrue(vm.trash.value.isEmpty())
    }

    @Test fun cancelledQueuedPasswordDoesNotCallTheDialogOrPublishSuccess() {
        val release = holdTransactions()
        var callbacks = 0
        try {
            vm.edit("Queued edit") { it.copy(description = "Cancelled edit") }
            vm.changePassword("", "new-password") { callbacks++ }
            shadowOf(Looper.getMainLooper()).idle()
            owner.clear()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, callbacks)
        assertTrue(messages.isEmpty())
        runBlocking { assertFalse(repository.getProjectById(initial.id)!!.isPasswordProtected) }
    }

    @Test fun closingBeforeQueuedPasswordCompletesDoesNotCallTheOldDialog() {
        val release = holdTransactions()
        var callbacks = 0
        try {
            vm.edit("Before password") { it.copy(description = "Kept") }
            vm.changePassword("", "new-password") { callbacks++ }
            shadowOf(Looper.getMainLooper()).idle()
            vm.closeProject()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, callbacks)
        assertNull(vm.project.value)
        runBlocking { assertTrue(repository.verifyProjectPassword(initial.id, "new-password")) }
    }

    @Test fun protectedOpenReadFailureIsVisibleAndWrongPasswordDoesNotOpenTheProject() {
        runBlocking { assertTrue(repository.setProjectPassword(initial.id, null, "new-password")) }
        vm.closeProject()
        await { vm.busy == null }
        var wrong = 0
        vm.openProtectedProject(initial.id, "wrong") { wrong++ }
        await { vm.busy == null }
        assertEquals(1, wrong)
        assertNull(vm.project.value)
        db.openHelper.writableDatabase.execSQL("UPDATE projects SET objectTypesJson = 'broken'")
        vm.openProtectedProject(initial.id, "new-password") { wrong++ }
        await { vm.busy == null }
        assertEquals(1, wrong)
        assertNull(vm.project.value)
        assertTrue(messages.any { it.isError })
        db.openHelper.writableDatabase.execSQL("UPDATE projects SET objectTypesJson = '[]'")
        vm.openProtectedProject(initial.id, "new-password") { wrong++ }
        await { vm.busy == null }
        assertTrue(vm.project.value!!.isPasswordProtected)
    }

    @Test fun editOutcomeReportsFailureThenOneCommittedRetryWithUndo() {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_edit BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'edit save denied'); END")
        val outcomes = mutableListOf<String?>()
        vm.edit("Retried edit", { outcomes += it }) { it.copy(description = "Retained draft") }
        await { vm.busy == null }
        assertEquals(1, outcomes.size)
        assertTrue(outcomes.single()!!.contains("edit save denied"))
        assertEquals(initial, vm.project.value)
        runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
        assertTrue(messages.none { it.undo != null })
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_edit")
        vm.edit("Retried edit", { outcomes += it }) { it.copy(description = "Retained draft") }
        await { vm.busy == null }
        assertEquals(2, outcomes.size)
        assertNull(outcomes.last())
        assertEquals("Retained draft", vm.project.value!!.description)
        messages.single { it.undo != null }.undo!!.invoke()
        await { vm.busy == null }
        assertEquals(initial, vm.project.value)
    }

    @Test fun unchangedEditAcknowledgesOnceWithoutAddingUndo() {
        val outcomes = mutableListOf<String?>()
        vm.edit("Unchanged", { outcomes += it }) { it }
        await { vm.busy == null }
        assertEquals(listOf<String?>(null), outcomes)
        assertEquals(initial, vm.project.value)
        assertTrue(messages.isEmpty())
    }

    @Test fun closingProjectSuppressesBothOldEditOutcomesAndErrors() {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_edit BEFORE UPDATE ON projects WHEN NEW.description = 'Fail' BEGIN SELECT RAISE(ABORT, 'late edit failure'); END")
        val release = holdTransactions()
        var callbacks = 0
        try {
            vm.edit("Old success", { callbacks++ }) { it.copy(description = "Saved") }
            vm.edit("Old failure", { callbacks++ }) { it.copy(description = "Fail") }
            shadowOf(Looper.getMainLooper()).idle()
            vm.closeProject()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, callbacks)
        assertTrue(messages.isEmpty())
        assertNull(vm.project.value)
        runBlocking { assertEquals("Saved", repository.getProjectById(initial.id)!!.description) }
    }

    @Test fun cancelledEditDoesNotReportAnOutcomeOrPublishSuccess() {
        val release = holdTransactions()
        var callbacks = 0
        try {
            vm.edit("Cancelled edit", { callbacks++ }) { it.copy(description = "Cancelled") }
            shadowOf(Looper.getMainLooper()).idle()
            owner.clear()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, callbacks)
        assertTrue(messages.isEmpty())
        runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
    }

    @Test fun disposedEditorDoesNotCloseOnLateCommittedSave() {
        val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val save = EditSave(vm, uiScope)
        var closes = 0
        val release = holdTransactions()
        try {
            save.save("Disposed editor", { closes++ }) { it.copy(description = "Committed") }
            shadowOf(Looper.getMainLooper()).idle()
            uiScope.cancel()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, closes)
        assertNull(save.error)
        runBlocking { assertEquals("Committed", repository.getProjectById(initial.id)!!.description) }
    }

    @Test fun disposedEditorDoesNotReceiveLateSaveFailure() {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_disposed BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'disposed save denied'); END")
        val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val save = EditSave(vm, uiScope)
        var closes = 0
        val release = holdTransactions()
        try {
            save.save("Disposed failure", { closes++ }) { it.copy(description = "Rejected") }
            shadowOf(Looper.getMainLooper()).idle()
            uiScope.cancel()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, closes)
        assertNull(save.error)
        assertEquals(initial, vm.project.value)
        runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
    }
}
