package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Site
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ProjectCreationTest {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var db: AppDatabase
    private lateinit var repository: ProjectRepository
    private val project = Project(name = "New site", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(Site(name = "Building")))

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().setTransactionExecutor(executor).build()
        repository = ProjectRepository(db)
    }
    @After fun close() { db.close(); executor.shutdownNow() }

    @Test fun plainAndProtectedCreationPersistTheRequestedProtection() = runBlocking {
        repository.createProject(project, null)
        assertEquals(project, repository.getProjectById(project.id))
        assertNull(db.projectDao().getProjectById(project.id)!!.passwordHash)
        val protected = project.copy(id = java.util.UUID.randomUUID().toString(), sites = listOf(Site(name = "Protected building")))
        repository.createProject(protected, "dummy-password")
        assertTrue(repository.getProjectById(protected.id)!!.isPasswordProtected)
        assertTrue(repository.verifyProjectPassword(protected.id, "dummy-password"))
        assertFalse(repository.verifyProjectPassword(protected.id, "wrong"))
    }

    @Test fun inventoryFailureRollsBackProjectAndVerifierAndRetryCreatesOnlyOneCopy() = runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_creation BEFORE INSERT ON sites BEGIN SELECT RAISE(ABORT, 'isolated creation failure'); END")
        assertNotNull(runCatching { repository.createProject(project, "dummy-password") }.exceptionOrNull())
        assertNull(db.projectDao().getProjectById(project.id))
        assertTrue(db.inventoryDao().getSitesByProjectId(project.id).isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_creation")
        repository.createProject(project, "dummy-password")
        assertTrue(repository.verifyProjectPassword(project.id, "dummy-password"))
        assertNotNull(runCatching { repository.createProject(project, "dummy-password") }.exceptionOrNull())
        assertEquals(1, repository.getAllProjects().first().size)
    }

    @Test fun cancellationBeforeTransactionLeavesNoProjectAndAllowsRetry() = runBlocking {
        db.openHelper.writableDatabase
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        executor.execute { reached.countDown(); check(release.await(10, TimeUnit.SECONDS)) }
        assertTrue(reached.await(5, TimeUnit.SECONDS))
        try {
            val job = launch(start = CoroutineStart.UNDISPATCHED) { repository.createProject(project, null) }
            job.cancelAndJoin()
        } finally { release.countDown() }
        assertNull(repository.getProjectById(project.id))
        repository.createProject(project, "dummy-password")
        assertTrue(repository.verifyProjectPassword(project.id, "dummy-password"))
        assertEquals(1, repository.getAllProjects().first().size)
    }
}
