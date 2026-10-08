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
class PhotoCommandTest {
    @get:Rule val folder = TemporaryFolder()
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var vm: ProjectViewModel
    private lateinit var root: File
    private val owner = ViewModelStore()
    private val collector = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val messages = mutableListOf<UiMessage>()
    private val initial = Project(name = "Synthetic photo series", createdEpochMs = 1, updatedEpochMs = 1)

    @Before fun setUp() {
        root = folder.newFolder("media")
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().setTransactionExecutor(executor).build()
        repository = ProjectRepository(db, root, "isolated-photo-recovery")
        runBlocking { repository.saveProject(initial) }
        vm = ProjectViewModel(repository)
        owner.put("photo", vm)
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
        fail("Photo command did not finish")
    }

    private fun photo() = requireNotNull(vm.preparePhoto(AttachmentTargetType.PROJECT, initial.id)).apply {
        writeBytes(byteArrayOf(1, 2, 3))
    }

    private fun holdTransactions(): CountDownLatch {
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        executor.execute { reached.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        return release
    }

    @Test fun continuationCanPrepareTheNextPhotoAfterCommitAndCleanup() {
        val first = photo()
        var next: File? = null
        var calls = 0
        vm.onPhotoResult(true) { kept ->
            calls++
            assertTrue(kept)
            assertNull(vm.busy)
            assertEquals(1, vm.project.value!!.attachments.size)
            next = vm.preparePhoto(AttachmentTargetType.PROJECT, initial.id)
        }
        await { vm.busy == null }
        assertEquals(1, calls)
        assertNotNull(next)
        assertTrue(first.isFile)
        vm.onPhotoResult(false) {}
        await { vm.busy == null }
        assertEquals(1, AttachmentFiles.ownedFiles(root, initial.id).size)
        messages.single { it.undo != null }.undo!!.invoke()
        await { vm.busy == null }
        assertEquals(initial, vm.project.value)
        assertFalse(first.exists())
    }

    @Test fun failedPhotoReportsFalseOnlyAfterItsFileIsRemoved() {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_photo BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated photo denied'); END")
        val file = photo()
        var kept: Boolean? = null
        vm.onPhotoResult(true) { result -> assertFalse(file.exists()); kept = result }
        await { kept != null }
        assertEquals(false, kept)
        assertEquals(initial, vm.project.value)
        assertTrue(messages.single().isError)
    }

    @Test fun closedSessionSuppressesPhotoFailureAndContinuation() = verifyLate("close")
    @Test fun cancelledViewModelSuppressesPhotoContinuation() = verifyLate("cancel")
    private fun verifyLate(mode: String) {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_photo BEFORE UPDATE ON projects BEGIN SELECT RAISE(ABORT, 'isolated late photo denied'); END")
        val file = photo()
        var calls = 0
        val release = holdTransactions()
        try {
            vm.onPhotoResult(true) { calls++ }
            shadowOf(Looper.getMainLooper()).idle()
            if (mode == "close") vm.closeProject() else owner.clear()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertEquals(0, calls)
        assertTrue(messages.isEmpty())
        assertFalse(file.exists())
        runBlocking { assertEquals(initial, repository.getProjectById(initial.id)) }
    }

    @Test fun resultFromAnEarlierOpeningCannotAddPhotoToTheSameProject() {
        val file = photo()
        vm.closeProject()
        vm.openProject(initial.id)
        await { vm.busy == null }
        var calls = 0
        vm.onPhotoResult(true) { calls++ }
        await { vm.busy == null }
        assertEquals(0, calls)
        assertEquals(initial, vm.project.value)
        assertTrue(messages.isEmpty())
        assertFalse(file.exists())
    }
}
