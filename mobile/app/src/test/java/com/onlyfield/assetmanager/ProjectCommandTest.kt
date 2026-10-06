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
}
