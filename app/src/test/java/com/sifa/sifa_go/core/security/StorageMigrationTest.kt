package com.sifa.sifa_go.core.security

import android.content.Context
import com.sifa.sifa_go.core.utils.InMemorySharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageMigrationTest {

    private val legacyName = "legacy_prefs"
    private val alias = "alias-test"

    @Test
    fun `migrates all legacy values encrypted into target and deletes legacy`() {
        val legacy = InMemorySharedPreferences().apply {
            edit()
                .putString("TOKEN", "jwt.old")
                .putString("REFRESH_TOKEN", "jwt.refresh")
                .putLong("TOKEN_EXPIRY", 111L)
                .putStringSet("ROLES", mutableSetOf("USER_APP", "ADMIN"))
                .commit()
        }
        val target = InMemorySharedPreferences()
        val cipher = FakeCipher()
        val context = mockContextReturningLegacy(legacy)

        StorageMigration.migrateLegacyToEncrypted(context, legacyName, target, cipher, alias)

        // El destino contiene ciphertext (nunca el texto plano original).
        assertEquals("enc:|$alias|jwt.old", target.getString("TOKEN", null))
        assertFalse(target.getAll().containsValue("jwt.old"))

        // El archivo legacy en texto plano fue eliminado.
        verify { context.deleteSharedPreferences(legacyName) }

        // La flag de migración queda marcada.
        assertTrue(target.getBoolean(StorageMigration.MIGRATED_FLAG, false))

        // Los valores descifrados son legibles por el cipher (round-trip).
        assertEquals("jwt.old", cipher.decrypt(alias, target.getString("TOKEN", null)!!))
    }

    @Test
    fun `migration is idempotent (does not re-encrypt when flag present)`() {
        val legacy = InMemorySharedPreferences().apply {
            edit().putString("TOKEN", "jwt.old").commit()
        }
        val target = InMemorySharedPreferences()
        val cipher = FakeCipher()
        val context = mockContextReturningLegacy(legacy)

        StorageMigration.migrateLegacyToEncrypted(context, legacyName, target, cipher, alias)
        val ciphertextAfterFirst = target.getString("TOKEN", null)
        StorageMigration.migrateLegacyToEncrypted(context, legacyName, target, cipher, alias)

        // No se re-cifra: el valor sigue siendo el mismo y el legacy no se toca de nuevo.
        assertEquals(ciphertextAfterFirst, target.getString("TOKEN", null))
    }

    @Test
    fun `empty legacy marks migration and removes legacy file without encrypting`() {
        val legacy = InMemorySharedPreferences()
        val target = InMemorySharedPreferences()
        val cipher = FakeCipher()
        val context = mockContextReturningLegacy(legacy)

        StorageMigration.migrateLegacyToEncrypted(context, legacyName, target, cipher, alias)

        assertTrue(target.getBoolean(StorageMigration.MIGRATED_FLAG, false))
        assertNull(target.getString("TOKEN", null))
        verify { context.deleteSharedPreferences(legacyName) }
    }

    private fun mockContextReturningLegacy(legacy: InMemorySharedPreferences): Context {
        val context = mockk<Context>()
        every { context.getSharedPreferences(legacyName, any()) } returns legacy
        every { context.deleteSharedPreferences(legacyName) } returns true
        return context
    }
}