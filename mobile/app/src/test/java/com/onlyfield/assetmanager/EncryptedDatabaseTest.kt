package com.onlyfield.assetmanager

import com.onlyfield.assetmanager.data.local.EncryptedDatabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EncryptedDatabaseTest {
    @Test
    fun detectsPlaintextHeaderOnly() {
        val plain = File.createTempFile("plain", ".db").apply {
            writeBytes("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(100))
        }
        val encrypted = File.createTempFile("enc", ".db").apply { writeBytes(ByteArray(116) { it.toByte() }) }
        assertTrue(EncryptedDatabase.isPlaintextSqlite(plain))
        assertFalse(EncryptedDatabase.isPlaintextSqlite(encrypted))
        assertFalse(EncryptedDatabase.isPlaintextSqlite(File(plain.path + ".missing")))
    }
}
