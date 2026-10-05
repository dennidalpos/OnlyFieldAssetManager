package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.exchange.PackageSerializer
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.EventQueue
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import com.sun.nio.file.ExtendedOpenOption

class DesktopIoTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun workerLeavesAwtEventsResponsiveAndPropagatesFailures() {
        var busy = false
        DesktopIo { busy = it }.use { io ->
            EventQueue.invokeAndWait {
                val dispatched = CountDownLatch(1)
                val busySeen = AtomicReference<Boolean>()
                val result = io.run {
                    assertFalse(EventQueue.isDispatchThread())
                    EventQueue.invokeLater { busySeen.set(busy); dispatched.countDown() }
                    assertTrue(dispatched.await(5, TimeUnit.SECONDS))
                    42
                }
                assertEquals(42, result)
                assertEquals(true, busySeen.get())
                assertFalse(busy)
                val failure = runCatching { io.run { throw IOException("dummy failure") } }.exceptionOrNull()
                assertTrue(failure is IOException)
                assertFalse(busy)
            }
        }
    }

    private fun project(): Project {
        val area = Area(name = "Floor")
        return Project(name = "Large sample", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = true,
            sites = listOf(Site(name = "Site", areas = listOf(area), devices = (1..500).map { Device(technicalName = "SW-$it", areaId = area.id) })))
    }

    @Test fun protectedSavePreservesSynchronousResultWithoutAcceptingConcurrentEdits() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project(), password = "dummy-password")) }
        try {
            EventQueue.invokeAndWait {
                state.importFile(incoming, "dummy-password", compare = false)
                val before = state.project!!
                var eventSeen = false
                var changedDuringSave = false
                EventQueue.invokeLater {
                    eventSeen = state.busy
                    state.requestChange { changedDuringSave = true }
                }
                state.update(before.copy(name = "Saved on worker"), "Saved")
                assertTrue(eventSeen)
                assertFalse(changedDuringSave)
                assertFalse(state.busy)
                assertEquals("Saved on worker", state.project!!.name)
                assertTrue(state.canUndo)
            }
            val local = storage.listStoredProjects().single().file
            val saved = storage.importPackageFromFile(local, "dummy-password").pkg!!.project
            assertEquals("Saved on worker", saved.name)
            assertEquals(500, saved.sites.single().devices.size)
        } finally { state.shutdown() }
    }

    @Test fun failedWorkerSaveKeepsPreviousUiStateAndReenablesEdits() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val incoming = folder.newFile().apply { writeBytes(PackageSerializer.exportPackage(project(), password = "dummy-password")) }
        try {
            EventQueue.invokeAndWait { state.importFile(incoming, "dummy-password", compare = false) }
            val before = state.project!!
            val local = storage.listStoredProjects().single().file
            val original = local.readBytes()
            Files.newByteChannel(local.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
                EventQueue.invokeAndWait {
                    state.update(before.copy(name = "Must not commit"), "Changed")
                    assertEquals(before, state.project)
                    assertNotNull(state.error)
                    assertFalse(state.busy)
                    assertFalse(state.canUndo)
                    var allowed = false
                    state.requestChange { allowed = true }
                    assertTrue(allowed)
                }
            }
            assertArrayEquals(original, local.readBytes())
            assertFalse(storage.dataDir.walkTopDown().any { it.extension == "tmp" })
        } finally { state.shutdown() }
    }
}
