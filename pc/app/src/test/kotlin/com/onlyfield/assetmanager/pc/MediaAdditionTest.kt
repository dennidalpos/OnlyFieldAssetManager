package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.AttachmentClassification
import com.onlyfield.assetmanager.core.model.Project
import com.sun.nio.file.ExtendedOpenOption
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class MediaAdditionTest {
    @get:Rule val folder = TemporaryFolder()
    private fun source(bytes: Long) = folder.newFile("${java.util.UUID.randomUUID()}.bin").apply {
        RandomAccessFile(this, "rw").use { it.setLength(bytes) }
    }

    private fun exercise(check: (DesktopAppState, File) -> Unit) {
        for (password in listOf(null, "test-password")) {
            val storage = DesktopStorageManager(folder.newFolder())
            val project = Project(name = "Media", createdEpochMs = 0, updatedEpochMs = 0, isPasswordProtected = password != null)
            val file = storage.saveProjectLocally(project, password)
            val state = DesktopAppState(storage)
            try {
                state.importFile(file, password, compare = false)
                state.addAttachment(source(2L * 1024 * 1024), "Existing", AttachmentClassification.SHAREABLE)
                assertNull(state.error)
                check(state, file)
                state.update(state.project!!.copy(name = "Still editable"), "Rename")
                assertNull(state.error)
                assertEquals("Still editable", state.project!!.name)
                storage.importPackageFromFile(file, password).pkg!!.use { assertEquals(1, it.project.attachments.size) }
            } finally { state.shutdown() }
        }
    }

    @Test fun oversizedGenericAttachmentDoesNotPoisonMediaOrTheNextSave() = exercise { state, file ->
        val before = state.project!!
        val bytes = file.readBytes()
        val paths = state.storage.mediaSnapshot(before.id)!!.keys.toSet()
        state.addAttachment(source(33L * 1024 * 1024), "Rejected", AttachmentClassification.SHAREABLE)
        assertNotNull(state.error)
        assertEquals(before, state.project)
        assertEquals(paths, state.storage.mediaSnapshot(before.id)!!.keys)
        assertArrayEquals(bytes, file.readBytes())
        assertEquals(if (state.hasPassword) 0 else 1, state.storage.getMediaFolder().walkTopDown().count { it.isFile })
    }

    @Test fun failedGenericAndGeneratedMediaWritesRestoreThePreviousCatalogAndCache() = exercise { state, file ->
        val before = state.project!!
        val bytes = file.readBytes()
        val paths = state.storage.mediaSnapshot(before.id)!!.keys.toSet()
        Files.newByteChannel(file.toPath(), StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE).use {
            state.addAttachment(source(2L * 1024 * 1024), "Failed", AttachmentClassification.SHAREABLE)
            assertNotNull(state.error)
            assertEquals(before, state.project)
            assertEquals(paths, state.storage.mediaSnapshot(before.id)!!.keys)
            assertFalse(state.addMapSnapshot(DesktopMapSnapshot(byteArrayOf(1, 2, 3), "Test source"), "Failed snapshot"))
            assertEquals(before, state.project)
            assertEquals(paths, state.storage.mediaSnapshot(before.id)!!.keys)
            assertArrayEquals(bytes, file.readBytes())
        }
        assertEquals(if (state.hasPassword) 0 else 1, state.storage.getMediaFolder().walkTopDown().count { it.isFile })
    }
}
