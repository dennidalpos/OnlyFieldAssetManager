package com.onlyfield.assetmanager

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.ui.ProjectViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MediaAdditionTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private lateinit var root: File
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private lateinit var vm: ProjectViewModel
    private val owner = ViewModelStore()
    private val project = Project(name = "Media", createdEpochMs = 0, updatedEpochMs = 0)

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        directory = File(context.cacheDir, "media-${UUID.randomUUID()}").apply { mkdirs() }
        root = File(directory, "attachments").apply { mkdirs() }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repository = ProjectRepository(db, root, recoveryPassword = "dummy-recovery-key")
        runBlocking { repository.saveProject(project) }
        vm = ProjectViewModel(repository)
        owner.put("media", vm)
        vm.openProject(project.id)
        await { vm.project.value != null && vm.busy == null }
    }

    @After fun cleanUp() {
        owner.clear()
        shadowOf(Looper.getMainLooper()).idle()
        db.close()
        directory.deleteRecursively()
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        do {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        } while (System.nanoTime() < deadline)
        fail("Media operation did not finish")
    }

    private fun source(bytes: Long) = File(directory, "${UUID.randomUUID()}.bin").apply {
        RandomAccessFile(this, "rw").use { it.setLength(bytes) }
    }

    @Test fun oversizedGenericFileLeavesNoPayloadAndAValidAdditionStillSucceeds() {
        vm.addAttachment(context, Uri.fromFile(source(33L * 1024 * 1024)), "Rejected", AttachmentClassification.SHAREABLE)
        await { vm.busy == null }
        assertEquals(project, vm.project.value)
        assertEquals(0, root.walkTopDown().count { it.isFile })
        runBlocking { assertEquals(project, repository.getProjectById(project.id)) }
        vm.addAttachment(context, Uri.fromFile(source(1024)), "Accepted", AttachmentClassification.SHAREABLE)
        await { vm.busy == null && vm.project.value!!.attachments.size == 1 }
        runBlocking { assertEquals(vm.project.value, repository.getProjectById(project.id)) }
        assertEquals(1, root.walkTopDown().count { it.isFile })
    }

    @Test fun rejectedCameraPhotoStopsTheSeriesAndRemovesItsFile() {
        val file = vm.preparePhoto(AttachmentTargetType.PROJECT, project.id)!!
        RandomAccessFile(file, "rw").use { it.setLength(33L * 1024 * 1024) }
        var kept: Boolean? = null
        vm.onPhotoResult(true) { kept = it }
        await { kept != null }
        assertEquals(false, kept)
        assertFalse(file.exists())
        assertEquals(project, vm.project.value)
        runBlocking { assertEquals(project, repository.getProjectById(project.id)) }
    }

    @Test fun failedDatabaseSaveRollsBackGenericAndCameraMedia() {
        db.close()
        vm.addAttachment(context, Uri.fromFile(source(1024)), "Failed", AttachmentClassification.SHAREABLE)
        await { vm.busy == null }
        assertEquals(project, vm.project.value)
        assertEquals(0, root.walkTopDown().count { it.isFile })
        val photo = vm.preparePhoto(AttachmentTargetType.PROJECT, project.id)!!
        photo.writeBytes(byteArrayOf(1, 2, 3))
        var kept: Boolean? = null
        vm.onPhotoResult(true) { kept = it }
        await { kept != null }
        assertEquals(false, kept)
        assertFalse(photo.exists())
        assertEquals(project, vm.project.value)
    }

    @Test fun cancellationAfterTheDatabaseCommitStillRecordsOwnershipOfMedia() = runBlocking {
        val attachment = Attachment(name = "Committed", originalFileName = "committed.bin", relativePath = "")
        val current = project.copy(attachments = listOf(attachment))
        val file = com.onlyfield.assetmanager.exchange.AttachmentFiles.localFile(root, project.id, attachment)
        file.parentFile!!.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3))
        var committed = false
        lateinit var job: kotlinx.coroutines.Job
        job = launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            repository.saveMediaProject(current) { committed = true; job.cancel() }
        }
        job.start()
        job.join()
        assertTrue(committed)
        assertEquals(current, repository.getProjectById(project.id))
        assertTrue(file.exists())
    }
}
