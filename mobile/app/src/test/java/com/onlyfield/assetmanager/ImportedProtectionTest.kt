package com.onlyfield.assetmanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.ProjectPackage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ImportedProtectionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "import-${UUID.randomUUID()}.db"
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository

    @Before fun open() {
        db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        repository = ProjectRepository(db)
    }

    @After fun close() {
        db.close()
        context.deleteDatabase(databaseName)
    }

    private fun reopen() {
        db.close()
        open()
    }

    private fun incoming(project: Project, password: String? = null): ProjectPackage =
        requireNotNull(PackageSerializer.importPackage(
            PackageSerializer.exportPackage(project, password = password), password = password
        ).pkg)

    @Test fun protectedImportReopensAndRetainsProtectionAfterAnEdit() = runBlocking {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Incoming", isPasswordProtected = true)
        assertTrue(repository.importProjectPackage(incoming(project, "incoming-password"), "incoming-password"))
        reopen()
        assertTrue(repository.verifyProjectPassword(project.id, "incoming-password"))
        assertFalse(repository.verifyProjectPassword(project.id, "wrong-password"))
        repository.saveProject(project.copy(name = "Edited"))
        reopen()
        assertTrue(repository.verifyProjectPassword(project.id, "incoming-password"))
    }

    @Test fun replacementUsesIncomingPassword() = runBlocking {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Local")
        repository.saveProject(project)
        assertTrue(repository.setProjectPassword(project.id, null, "local-password"))
        val replacement = project.copy(name = "Replacement", isPasswordProtected = true)
        repository.importProjectPackage(incoming(replacement, "incoming-password"), "incoming-password")
        reopen()
        assertEquals("Replacement", repository.getProjectById(project.id)?.name)
        assertTrue(repository.verifyProjectPassword(project.id, "incoming-password"))
        assertFalse(repository.verifyProjectPassword(project.id, "local-password"))
    }

    @Test fun unprotectedReplacementClearsVerifier() = runBlocking {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Local")
        repository.saveProject(project)
        repository.setProjectPassword(project.id, null, "local-password")
        repository.importProjectPackage(incoming(project))
        reopen()
        assertFalse(repository.getProjectById(project.id)!!.isPasswordProtected)
        assertNull(db.projectDao().getProjectById(project.id)!!.passwordHash)
    }

    @Test fun missingPasswordLeavesLocalProjectUntouched() = runBlocking {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Local")
        repository.saveProject(project)
        val replacement = project.copy(name = "Replacement", isPasswordProtected = true)
        try {
            repository.importProjectPackage(incoming(replacement, "incoming-password"))
            fail("Protected import must require a password")
        } catch (_: IllegalArgumentException) {
            assertEquals("Local", repository.getProjectById(project.id)?.name)
        }
    }

    @Test fun mergePreservesLocalPasswordEvenWhenIncomingRemovesProtection() = runBlocking {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Local")
        repository.saveProject(project)
        repository.setProjectPassword(project.id, null, "local-password")
        repository.importMergedPackage(incoming(project), project.copy(name = "Merged"))
        reopen()
        assertEquals("Merged", repository.getProjectById(project.id)?.name)
        assertTrue(repository.getProjectById(project.id)!!.isPasswordProtected)
        assertTrue(repository.verifyProjectPassword(project.id, "local-password"))
        assertFalse(repository.verifyProjectPassword(project.id, "wrong-password"))
    }

    @Test fun mergeKeepsUnprotectedLocalCopyUnprotected() = runBlocking {
        val project = Project(createdEpochMs = 0, updatedEpochMs = 0, name = "Local")
        repository.saveProject(project)
        val protected = project.copy(isPasswordProtected = true)
        repository.importMergedPackage(incoming(protected, "incoming-password"), protected.copy(name = "Merged"))
        reopen()
        assertFalse(repository.getProjectById(project.id)!!.isPasswordProtected)
        assertNull(db.projectDao().getProjectById(project.id)!!.passwordHash)
    }
}
