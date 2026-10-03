package com.onlyfield.assetmanager.exchange

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Project password verifier: `pbkdf2-sha256$<iterations>$<saltHex>$<hashHex>`.
 * Iterations follow the OWASP Password Storage Cheat Sheet (600.000 for PBKDF2-HMAC-SHA256).
 */
object PasswordHasher {
    const val ITERATIONS = 600_000
    private const val PREFIX = "pbkdf2-sha256"
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256

    fun hash(password: String, iterations: Int = ITERATIONS): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "$PREFIX\$$iterations\$${salt.toHex()}\$${derive(password, salt, iterations).toHex()}"
    }

    /** Also accepts the pre-v1.1 unsalted SHA-256 hex digest. */
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size == 4 && parts[0] == PREFIX) {
            val iterations = parts[1].toIntOrNull() ?: return false
            return runCatching {
                MessageDigest.isEqual(derive(password, parts[2].hexToBytes(), iterations), parts[3].hexToBytes())
            }.getOrDefault(false)
        }
        val legacy = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return MessageDigest.isEqual(legacy.toHex().toByteArray(), stored.lowercase().toByteArray())
    }

    /** True for legacy digests or iteration counts below the current one. */
    fun needsRehash(stored: String): Boolean {
        val parts = stored.split('$')
        return parts.size != 4 || parts[0] != PREFIX || (parts[1].toIntOrNull() ?: 0) < ITERATIONS
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS)).encoded

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
