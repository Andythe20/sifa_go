package com.sifa.sifa_go.core.utils

import com.sifa.sifa_go.core.security.FakeCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    private val cipher = FakeCipher()
    private val prefs = InMemorySharedPreferences()
    private val sessionManager = SessionManager(prefs, cipher, "alias-test")

    @Test
    fun `saveSession persistes encrypted values readable back`() {
        sessionManager.saveSession(
            token = "jwt.access.1",
            refreshToken = "jwt.refresh.1",
            username = "user@example.com",
            roles = listOf("USER_APP", "ROLE_X"),
            expiry = 1_752_000_000L,
            iat = 1_751_000_000L,
        )

        // En el archivo solo debe existir ciphertext, nunca el valor original.
        assertEquals("enc:|alias-test|jwt.access.1", prefs.getString("TOKEN", null))
        assertFalse(prefs.getAll().containsValue("jwt.access.1"))

        assertEquals("jwt.access.1", sessionManager.getToken())
        assertEquals("jwt.refresh.1", sessionManager.getRefreshToken())
        assertEquals("user@example.com", sessionManager.getUsername())
        assertEquals(listOf("USER_APP", "ROLE_X"), sessionManager.getRoles())
        assertEquals(1_752_000_000L, sessionManager.getTokenExpiry())
        assertEquals(1_751_000_000L, sessionManager.getTokenIat())
    }

    @Test
    fun `hasValidSession is true only with token refresh and USER_APP role`() {
        sessionManager.saveSession(
            token = "t",
            refreshToken = "rt",
            username = "u",
            roles = listOf("USER_APP"),
        )
        assertTrue(sessionManager.hasValidSession())

        val differentRoleManager = SessionManager(
            prefs = InMemorySharedPreferences(),
            cipher = FakeCipher(),
            keyAlias = "alias-test",
        ).also {
            it.saveSession("t", "rt", "u", listOf("ADMIN"))
        }
        assertFalse(differentRoleManager.hasValidSession())
    }

    @Test
    fun `missing values return defaults`() {
        assertNull(sessionManager.getToken())
        assertNull(sessionManager.getRefreshToken())
        assertNull(sessionManager.getUsername())
        assertTrue(sessionManager.getRoles().isEmpty())
        assertEquals(0L, sessionManager.getTokenExpiry())
        assertEquals(0L, sessionManager.getTokenIat())
    }

    @Test
    fun `logout clears session but keeps migration flag`() {
        sessionManager.saveSession("t", "rt", "u", listOf("USER_APP"))

        sessionManager.logout()

        assertNull(sessionManager.getToken())
        assertFalse(sessionManager.hasValidSession())
        assertTrue(prefs.getBoolean(com.sifa.sifa_go.core.security.StorageMigration.MIGRATED_FLAG, false))
    }

    @Test
    fun `decrypt failure does not crash and hides session`() {
        sessionManager.saveSession("t", "rt", "u", listOf("USER_APP"))
        cipher.failDecrypt = true

        assertNull(sessionManager.getToken())
        assertEquals(0L, sessionManager.getTokenExpiry())
    }
}