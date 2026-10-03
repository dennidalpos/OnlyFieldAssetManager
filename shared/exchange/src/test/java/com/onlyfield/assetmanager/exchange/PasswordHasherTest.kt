package com.onlyfield.assetmanager.exchange

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun verifiesSaltedHash() {
        val a = PasswordHasher.hash("Segreta1", iterations = 1_000)
        val b = PasswordHasher.hash("Segreta1", iterations = 1_000)
        assertNotEquals(a, b) // random salt
        assertTrue(PasswordHasher.verify("Segreta1", a))
        assertFalse(PasswordHasher.verify("segreta1", a))
        assertTrue(PasswordHasher.needsRehash(a)) // below current iterations
        assertFalse(PasswordHasher.needsRehash(PasswordHasher.hash("x")))
    }

    @Test
    fun acceptsLegacySha256AndRejectsMalformed() {
        // SHA-256("password")
        val legacy = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
        assertTrue(PasswordHasher.verify("password", legacy))
        assertFalse(PasswordHasher.verify("wrong", legacy))
        assertTrue(PasswordHasher.needsRehash(legacy))
        assertFalse(PasswordHasher.verify("password", "pbkdf2-sha256\$1000\$zz\$00"))
    }
}
