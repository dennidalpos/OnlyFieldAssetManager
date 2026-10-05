package com.onlyfield.assetmanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.Attachment
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.exchange.PackageSerializer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class AttachmentConfinementTest {
    private lateinit var db: AppDatabase
    private lateinit var directory: File
    private lateinit var root: File
    private lateinit var repository: ProjectRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        directory = File(context.cacheDir, "confinement-${UUID.randomUUID()}").apply { mkdirs() }
        root = File(directory, "attachments").apply { mkdirs() }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = ProjectRepository(db, root)
    }

    @After fun tearDown() {
        db.close()
        directory.deleteRecursively()
    }

    @Test fun importedPathsCannotReadOrExportExternalFiles() = runBlocking {
        val privateFile = File(directory, "private/dummy.txt").apply {
            parentFile!!.mkdirs()
            writeText("EXTERNAL-DUMMY")
        }
        for (path in listOf("private/dummy.txt", "attachments/../private/dummy.txt", "../private/dummy.txt", privateFile.absolutePath)) {
            val attachment = Attachment(name = "Missing", originalFileName = "photo.jpg", relativePath = path)
            val project = Project(name = "Incoming", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(attachment))
            val evaluation = repository.evaluateImportPackage(ByteArrayInputStream(PackageSerializer.exportPackage(project)))
            assertTrue(evaluation.importResult.validationResult.isValid)
            repository.importProjectPackage(evaluation.importResult.pkg!!)
            assertNull(repository.attachmentFile(project.id, attachment))
            assertEquals(listOf(attachment), repository.missingAttachments(project))
            val exported = PackageSerializer.importPackage(repository.exportProjectPackage(project.id)!!).pkg!!
            assertTrue(exported.attachments.isEmpty())
            assertEquals("EXTERNAL-DUMMY", privateFile.readText())
        }
    }

    @Test fun legitimatePayloadStillExtractsAndExports() = runBlocking {
        val attachment = Attachment(name = "Photo", originalFileName = "photo.jpg", relativePath = "attachments/legacy.jpg")
        val project = Project(name = "Incoming", createdEpochMs = 0, updatedEpochMs = 0, attachments = listOf(attachment))
        val bytes = "PHOTO-DUMMY".toByteArray()
        val incoming = PackageSerializer.importPackage(PackageSerializer.exportPackage(project, mapOf(attachment.relativePath to bytes))).pkg!!
        repository.importProjectPackage(incoming)
        assertArrayEquals(bytes, repository.attachmentFile(project.id, attachment)!!.readBytes())
        assertTrue(repository.missingAttachments(project).isEmpty())
        val exported = PackageSerializer.importPackage(repository.exportProjectPackage(project.id)!!).pkg!!
        assertArrayEquals(bytes, exported.attachments[AttachmentFiles.entryName(attachment)])
    }
}
