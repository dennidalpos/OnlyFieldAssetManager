package com.onlyfield.assetmanager

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.repository.ProjectRepository
import com.onlyfield.assetmanager.exchange.PackageSerializer
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class ProjectReadTransactionTest {
    @Test fun loadAndExportCannotMixProjectRowsWithNewInventory() = runBlocking {
        val pause = AtomicBoolean(false)
        var reached = CountDownLatch(1)
        var release = CountDownLatch(1)
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().setQueryCallback({ sql, _ ->
                if (sql.startsWith("SELECT * FROM sites") && pause.compareAndSet(true, false)) {
                    reached.countDown()
                    check(release.await(10, TimeUnit.SECONDS))
                }
            }, Executor { it.run() }).build()
        try {
            val repository = ProjectRepository(db)
            val before = Project(name = "Before", createdEpochMs = 0, updatedEpochMs = 0,
                sites = listOf(Site(name = "Old site", devices = listOf(Device(technicalName = "Old device")))))
            val after = before.copy(name = "After", sites = listOf(Site(name = "New site", devices = listOf(Device(technicalName = "New device")))))
            for (export in listOf(false, true)) {
                repository.saveProject(before)
                reached = CountDownLatch(1)
                release = CountDownLatch(1)
                pause.set(true)
                val read = async(Dispatchers.IO) {
                    if (!export) repository.getProjectById(before.id)
                    else PackageSerializer.importPackage(ByteArrayInputStream(repository.exportProjectPackage(before.id)!!)).pkg!!.use { it.project }
                }
                try {
                    assertTrue(reached.await(5, TimeUnit.SECONDS))
                    val writeStarted = CompletableDeferred<Unit>()
                    val write = async(Dispatchers.IO) { writeStarted.complete(Unit); repository.saveProject(after) }
                    writeStarted.await()
                    release.countDown()
                    assertEquals(before, read.await())
                    write.await()
                    assertEquals(after, repository.getProjectById(before.id))
                } finally { release.countDown() }
            }
        } finally { db.close() }
    }
}
