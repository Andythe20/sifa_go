package com.sifa.sifa_go.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas del cifrado real con Android Keystore.
 * Requieren device o emulador: el KeyStore hardware no se puede simular en JVM.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreAesCipherInstrumentedTest {

    private val cipher = KeystoreAesCipher()
    private val alias = "sifa_keystore_master_key_test"

    @Test
    fun encryptThenDecryptReturnsOriginal() {
        val secret = "jwt.access.token.muy.secreto"
        val encrypted = cipher.encrypt(alias, secret)

        // El ciphertext jamás debe contener el texto plano.
        assertNotEquals(secret, encrypted)
        assertEquals(secret, cipher.decrypt(alias, encrypted))
    }

    @Test
    fun samePlaintextProducesDifferentCiphertexts() {
        // GCM usa un IV aleatorio por operación: cifrados del mismo valor difieren.
        val first = cipher.encrypt(alias, "mismo-valor")
        val second = cipher.encrypt(alias, "mismo-valor")

        assertNotEquals(first, second)
        assertEquals("mismo-valor", cipher.decrypt(alias, first))
        assertEquals("mismo-valor", cipher.decrypt(alias, second))
    }

    @Test
    fun tamperedCiphertextReturnsNull() {
        val encrypted = cipher.encrypt(alias, "dato.original")
        // Corrompemos el último byte (parte del tag GCM) -> debe fallar la autenticación.
        val corrupted = encrypted.take(encrypted.length - 1) +
            if (encrypted.last() == 'A') 'B' else 'A'

        assertNull(cipher.decrypt(alias, corrupted))
    }

    @Test
    fun decryptWithUnknownAliasReturnsNull() {
        val value = cipher.encrypt(alias, "dato")
        assertNull(cipher.decrypt("alias_inexistente", value))
    }
}