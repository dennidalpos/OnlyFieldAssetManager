package com.onlyfield.assetmanager.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
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

    /** Greenfield schema (v1): any other on-disk version is dropped and recreated. */
    fun open(context: Context): AppDatabase {
        System.loadLibrary("sqlcipher")
        val app = context.applicationContext
        val key = ("x'" + loadOrCreateKey(app).toHex() + "'").toByteArray(Charsets.US_ASCII)
        return Room.databaseBuilder(app, AppDatabase::class.java, DB_NAME)
            .openHelperFactory(SupportOpenHelperFactory(key))
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()
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
