package com.onlyfield.assetmanager

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.ui.ImportState
import com.onlyfield.assetmanager.ui.ProjectViewModel
import kotlinx.coroutines.runBlocking
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
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ImportLifecycleTest {
    @get:Rule val folder = TemporaryFolder()
    private val executor = Executors.newSingleThreadExecutor()
    private val owner = ViewModelStore()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var vm: ProjectViewModel
    private val attachment = Attachment(name = "Photo", originalFileName = "photo.bin", relativePath = "")
    private val project = Project(name = "Incoming", createdEpochMs = 1, updatedEpochMs = 1, attachments = listOf(attachment))
    private val path get() = AttachmentFiles.entryName(attachment)

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
            .setTransactionExecutor(executor).build()
        repository = ProjectRepository(db, folder.newFolder("media"))
        vm = ProjectViewModel(repository)
        owner.put("import", vm)
    }

    @After fun tearDown() {
        owner.clear()
        await { vm.busy == null }
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
        fail("Import did not finish")
    }

    private fun source(value: Project = project, password: String? = null): Uri {
        val file = folder.newFile()
        file.writeBytes(PackageSerializer.exportPackage(value, mapOf(path to ByteArray(2 * 1024 * 1024) { 42 }), password))
        return Uri.fromFile(file)
    }

    private fun holdTransactions(): CountDownLatch {
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        executor.execute { reached.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        return release
    }

    private fun staging(): Set<File> = File(requireNotNull(System.getProperty("java.io.tmpdir"))).listFiles().orEmpty()
        .filter { it.isDirectory && it.name.startsWith("ofam-import-") }.toSet()

    private fun awaitWorker(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!condition()) {
            check(System.nanoTime() < deadline) { "Worker did not reach the ownership boundary" }
            Thread.sleep(5)
        }
    }

    @Test fun cancelDuringReadClosesTheStreamAndCannotPublishReview() {
        val uri = Uri.parse("content://import-test/blocked")
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        val closed = AtomicInteger()
        val bytes = PackageSerializer.exportPackage(project, mapOf(path to byteArrayOf(42)))
        val stream = object : ByteArrayInputStream(bytes) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                reached.countDown()
                check(release.await(10, TimeUnit.SECONDS))
                return super.read(b, off, len)
            }
            override fun close() { closed.incrementAndGet(); super.close() }
        }
        shadowOf(context.contentResolver).registerInputStream(uri, stream)
        vm.startImport(context.contentResolver, uri)
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        try { vm.cancelImport() } finally { release.countDown() }
        await { vm.busy == null }
        assertNull(vm.importState.value)
        assertTrue(closed.get() > 0)
        runBlocking { assertNull(repository.getProjectById(project.id)) }
    }

    @Test fun cancellationDuringRealPasswordDerivationCannotPublishOrRetainStaging() {
        val uri = Uri.parse("content://import-test/kdf")
        val bytes = PackageSerializer.exportPackage(project.copy(isPasswordProtected = true),
            mapOf(path to ByteArray(2 * 1024 * 1024) { 42 }), "secret")
        val worker = AtomicReference<Thread>()
        shadowOf(context.contentResolver).registerInputStream(uri, object : ByteArrayInputStream(bytes) {
            override fun close() { worker.set(Thread.currentThread()); super.close() }
        })
        val before = staging()
        vm.startImport(context.contentResolver, uri, "secret")
        awaitWorker { worker.get()?.stackTrace?.any { it.className.contains("PBKDF2") } == true }
        val created = staging() - before
        assertTrue(created.isNotEmpty())
        vm.cancelImport()
        await { vm.busy == null }
        assertNull(vm.importState.value)
        assertTrue(created.all { !it.exists() })
        runBlocking { assertNull(repository.getProjectById(project.id)) }
    }

    @Test fun supersededProtectedImportAndClearedViewModelCannotPublishLateState() {
        val protectedSource = source(project.copy(isPasswordProtected = true), "secret")
        val release = holdTransactions()
        try {
            vm.startImport(context.contentResolver, protectedSource, "secret")
            val other = project.copy(id = java.util.UUID.randomUUID().toString(), name = "Second")
            vm.startImport(context.contentResolver, source(other))
        } finally { release.countDown() }
        await { vm.busy == null }
        val review = vm.importState.value as ImportState.Review
        val pkg = review.evaluation.importResult.pkg!!
        assertEquals("Second", pkg.project.name)
        assertArrayEquals(ByteArray(2 * 1024 * 1024) { 42 }, pkg.attachments[path])
        owner.clear()
        await { vm.busy == null }
        assertNull(vm.importState.value)
        assertTrue(runCatching { pkg.attachments[path] }.exceptionOrNull() is IllegalStateException)
    }

    @Test fun clearingWhileComparisonWaitsReleasesEncryptedStaging() {
        val uri = source()
        val before = staging()
        val release = holdTransactions()
        var created = emptySet<File>()
        try {
            vm.startImport(context.contentResolver, uri)
            awaitWorker { (staging() - before).also { created = it }.isNotEmpty() }
            assertNotNull(vm.busy)
            owner.clear()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertNull(vm.importState.value)
        assertTrue(created.all { !it.exists() })
        runBlocking { assertNull(repository.getProjectById(project.id)) }
    }

    @Test fun cancelledWorkerReturnClosesThePackageBeforeReviewReceivesOwnership() {
        for (password in listOf(null, "secret")) {
            val uri = source(project.copy(isPasswordProtected = password != null), password)
            val before = staging()
            vm.startImport(context.contentResolver, uri, password)
            // Leave the main queue paused until the closeable result is ready to return.
            awaitWorker { !shadowOf(Looper.getMainLooper()).isIdle && (staging() - before).isNotEmpty() }
            val created = staging() - before
            vm.cancelImport()
            await { vm.busy == null }
            assertNull(vm.importState.value)
            assertTrue(created.all { !it.exists() })
        }
    }

    @Test fun cancellationBeforeTheConfirmCommitDoesNotSaveOrLeakThePackage() {
        vm.startImport(context.contentResolver, source())
        await { vm.busy == null }
        val pkg = (vm.importState.value as ImportState.Review).evaluation.importResult.pkg!!
        val release = holdTransactions()
        try {
            vm.confirmImport()
            vm.cancelImport()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertNull(vm.importState.value)
        assertNull(vm.project.value)
        assertTrue(runCatching { pkg.attachments[path] }.exceptionOrNull() is IllegalStateException)
        runBlocking { assertNull(repository.getProjectById(project.id)) }
    }

    @Test fun cancelledMergeAndRepeatedMergeCannotReuseOrPublishThePackage() {
        runBlocking { repository.saveProject(project.copy(name = "Local")) }
        vm.startImport(context.contentResolver, source())
        await { vm.busy == null }
        val pkg = (vm.importState.value as ImportState.Review).evaluation.importResult.pkg!!
        val release = holdTransactions()
        try {
            vm.startMerge()
            vm.startMerge()
            vm.cancelImport()
        } finally { release.countDown() }
        await { vm.busy == null }
        assertNull(vm.importState.value)
        assertTrue(runCatching { pkg.attachments[path] }.exceptionOrNull() is IllegalStateException)
        runBlocking { assertEquals("Local", repository.getProjectById(project.id)!!.name) }
    }

    @Test fun doubleConfirmationCommitsOnceAndClosesThePackageAfterSlowSave() {
        vm.startImport(context.contentResolver, source())
        await { vm.busy == null }
        val pkg = (vm.importState.value as ImportState.Review).evaluation.importResult.pkg!!
        db.openHelper.writableDatabase.execSQL("CREATE TABLE import_commits (id TEXT)")
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER count_import AFTER INSERT ON sync_snapshots BEGIN INSERT INTO import_commits VALUES (NEW.projectId); END")
        val release = holdTransactions()
        try {
            vm.confirmImport()
            vm.confirmImport()
            assertNotNull(vm.busy)
            assertNull(vm.importState.value)
            assertArrayEquals(ByteArray(2 * 1024 * 1024) { 42 }, pkg.attachments[path])
        } finally { release.countDown() }
        await { vm.busy == null }
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM import_commits").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        runBlocking { assertEquals(project, repository.getProjectById(project.id)) }
        assertTrue(runCatching { pkg.attachments[path] }.exceptionOrNull() is IllegalStateException)
    }
}
