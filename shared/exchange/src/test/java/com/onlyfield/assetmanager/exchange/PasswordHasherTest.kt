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
    fun rejectsUnsaltedDigestsAndMalformed() {
        // SHA-256("password"): unsalted digests are no longer a valid verifier.
        val unsalted = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
        assertFalse(PasswordHasher.verify("password", unsalted))
        assertFalse(PasswordHasher.verify("password", "pbkdf2-sha256\$1000\$zz\$00"))
    }
}
