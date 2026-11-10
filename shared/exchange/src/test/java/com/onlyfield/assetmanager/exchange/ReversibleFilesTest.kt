package com.onlyfield.assetmanager.exchange

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ReversibleFilesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun laterReplacementFailureRestoresExistingAndRemovedFiles() {
        val old = folder.newFile("old").apply { writeText("old bytes") }
        val removed = folder.newFile("removed").apply { writeText("recoverable bytes") }
        val added = File(folder.root, "added")
        val blocked = folder.newFolder("blocked")
        assertNotNull(runCatching {
            ReversibleFiles().use { files ->
                files.replace(old) { it.write("incoming".toByteArray()) }
                files.remove(removed)
                files.replace(added) { it.write(byteArrayOf(1)) }
                files.replace(blocked) { it.write(byteArrayOf(2)) }
                files.apply()
                files.commit()
            }
        }.exceptionOrNull())
        assertEquals("old bytes", old.readText())
        assertEquals("recoverable bytes", removed.readText())
        assertFalse(added.exists())
        assertEquals(setOf("old", "removed", "blocked"), folder.root.list()!!.toSet())
    }

    @Test fun stagingFailureAndFailureAfterApplyBothRestoreOldBytes() {
        val old = folder.newFile("payload").apply { writeText("previous") }
        for (afterApply in listOf(false, true)) {
            assertTrue(runCatching {
                ReversibleFiles().use { files ->
                    files.replace(old) {
                        it.write("incoming".toByteArray())
                        if (!afterApply) throw IOException("extraction failed")
                    }
                    files.apply()
                    throw IOException("database commit failed")
                }
            }.exceptionOrNull() is IOException)
            assertEquals("previous", old.readText())
            assertEquals(listOf("payload"), folder.root.list()!!.toList())
        }
    }

    @Test fun commitKeepsNewBytesAndRemovesRecoveryFiles() {
        val old = folder.newFile("payload").apply { writeText("previous") }
        ReversibleFiles().use { files ->
            files.replace(old) { it.write("incoming".toByteArray()) }
            files.apply()
            assertTrue(files.commit().isEmpty())
        }
        assertEquals("incoming", old.readText())
        assertEquals(listOf("payload"), folder.root.list()!!.toList())
    }
}
