package com.onlyfield.assetmanager.data.local

import com.onlyfield.assetmanager.core.i18n.Messages
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Opens SQLCipher with a Keystore-wrapped key. */
object EncryptedDatabase {
    const val DB_NAME = "onlyfield_asset_manager.db"
    private const val KEYSTORE_ALIAS = "ofam_db_key_wrap"
    private const val KEY_FILE = "db_key.bin"
    private const val GCM_IV_BYTES = 12

    fun open(context: Context, i18n: Messages = Messages()): AppDatabase {
        System.loadLibrary("sqlcipher")
        val app = context.applicationContext
        val keyLiteral = "x'" + loadOrCreateKey(app).toHex() + "'"
        migratePlaintext(app.getDatabasePath(DB_NAME), keyLiteral)
        backupBeforeUpgrade(app, keyLiteral, i18n)
        return Room.databaseBuilder(app, AppDatabase::class.java, DB_NAME)
            .openHelperFactory(SupportOpenHelperFactory(keyLiteral.toByteArray(Charsets.US_ASCII)))
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
    }

    private fun backupBeforeUpgrade(context: Context, keyLiteral: String, i18n: Messages) {
        val source = context.getDatabasePath(DB_NAME)
        if (!source.isFile) return
        val version = SQLiteDatabase.openDatabase(source.path, keyLiteral.toByteArray(Charsets.US_ASCII), null, SQLiteDatabase.OPEN_READWRITE, null, null).use { db ->
            if (db.version < 14) db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", emptyArray()).use { cursor ->
                check(cursor.moveToFirst() && (cursor.getInt(0) == 0)) { i18n.text("database.backupBusy") }
            }
            db.version
        }
        if (version >= 14) return
        val backup = File(context.noBackupFilesDir, "$DB_NAME.v$version.backup")
        if (!backup.exists()) {
            val temporary = File(backup.path + ".tmp")
            try {
                Files.copy(source.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING)
                Files.move(temporary.toPath(), backup.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } finally { temporary.delete() }
        }
    }

    /** True for a plaintext SQLite header. */
    fun isPlaintextSqlite(file: File): Boolean {
        if (!file.isFile || (file.length() < 16)) return false
        val header = ByteArray(16)
        file.inputStream().use { if (it.read(header) != 16) return false }
        return header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
    }

    /** Converts a legacy plaintext database before swapping it. */
    private fun migratePlaintext(dbFile: File, keyLiteral: String) {
        if (!isPlaintextSqlite(dbFile)) return
        val tmp = File(dbFile.path + ".enc").apply { delete() }
        val version = SQLiteDatabase.openDatabase(
            dbFile.path, "", null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY, null, null,
        ).use { plain ->
            plain.execSQL("ATTACH DATABASE ? AS encrypted KEY $keyLiteral", arrayOf(tmp.path))
            plain.rawExecSQL("SELECT sqlcipher_export('encrypted')")
            plain.execSQL("DETACH DATABASE encrypted", emptyArray())
            plain.version
        }
        SQLiteDatabase.openDatabase(
            tmp.path, keyLiteral.toByteArray(Charsets.US_ASCII), null, SQLiteDatabase.OPEN_READWRITE, null, null,
        ).use { encrypted ->
            encrypted.version = version
        }
        listOf("-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        Files.move(tmp.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /** 32-byte key stored as AES-GCM ciphertext. */
    private fun loadOrCreateKey(context: Context): ByteArray {
        val file = File(context.noBackupFilesDir, KEY_FILE)
        val wrapKey = keystoreKey()
        if (file.isFile) {
            val blob = file.readBytes()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, wrapKey, GCMParameterSpec(128, blob, 0, GCM_IV_BYTES))
            return cipher.doFinal(blob, GCM_IV_BYTES, blob.size - GCM_IV_BYTES)
        }
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrapKey) // Keystore generates the IV
        val tmp = File(file.path + ".tmp")
        tmp.writeBytes(cipher.iv + cipher.doFinal(key))
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        return key
    }

    private fun keystoreKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
