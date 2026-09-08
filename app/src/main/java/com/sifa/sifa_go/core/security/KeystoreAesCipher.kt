package com.sifa.sifa_go.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Abstracción del mecanismo de cifrado a nivel aplicación.
 *
 * Mantener el cifrado detrás de una interfaz permite:
 *  - inyectar una implementación falsa en las pruebas unitarias JVM (el Keystore
 *    real no se puede simular sin device/emulador),
 *  - evolucionar la implementación (p. ej. a Google Tink) sin tocar los repositorios.
 */
interface SecureCipher {

    /**
     * Cifra [plainText] usando la clave maestro del Keystore identificada por [alias].
     * Devuelve el resultado en Base64 (NO_WRAP): `Base64(IV || ciphertext)`.
     */
    fun encrypt(alias: String, plainText: String): String

    /**
     * Descifra [cipherText] previamente generado con [encrypt].
     * Devuelve `null` si la clave fue invalidada, el dato está corrupto o fue
     * manipulado (falla la autenticación AEAD/GCM).
     */
    fun decrypt(alias: String, cipherText: String): String?
}

/**
 * Implementación basada en **Android Keystore** (recomendación oficial actual de Google,
 * dado que androidx.security:security-crypto / EncryptedSharedPreferences fue deprecada).
 *
 * - La clave maestro AES-256 se genera y vive dentro del KeyStore (TEE/StrongBox):
 *   jamás sale del hardware, por lo que el ciphertext en disco es inútil sin el
 *   dispositivo original.
 * - Se utiliza AES/GCM/NoPadding (modo autenticado: detecta manipulación/corrupción).
 * - La clave NO está ligada a autenticación biométrica: la biometría es una feature
 *   de la app y, además, así la clave sobrevive el cambio de huellas/face.
 * - Formato del ciphertext persistido: `Base64(IV[12] || ciphertext)`; el tag GCM
 *   (128 bits) queda incluido al final del ciphertext producido por el Cipher.
 */
class KeystoreAesCipher : SecureCipher {

    /** Bloqueo para evitar generar la clave dos veces bajo el mismo alias (race). */
    private val keyLock = Any()

    override fun encrypt(alias: String, plainText: String): String {
        val key = getOrCreateKey(alias)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        // IV aleatorio por operación (generado por el Cipher): requerido por GCM.
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val payload = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, payload, iv.size, encryptedBytes.size)

        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    override fun decrypt(alias: String, cipherText: String): String? {
        return try {
            val key = getKey(alias) ?: return null
            val payload = Base64.decode(cipherText, Base64.NO_WRAP)

            // Los primeros 12 bytes son el IV; el resto es ciphertext + tag GCM.
            val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
            val encryptedData = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(encryptedData), Charsets.UTF_8)
        } catch (e: javax.crypto.AEADBadTagException) {
            // El ciphertext fue manipulado/corrupo (falló la autenticación GCM).
            // NO invalidamos la clave maestro: borrarla inhabilitaría el resto de
            // datos cifrados de la app solo por un dato dañado.
            null
        } catch (e: android.security.keystore.KeyPermanentlyInvalidatedException) {
            // La clave fue eliminada del Keystore (restauración del sistema,
            // reset de Keystore, etc.). La invalidamos localmente y devolvemos
            // null para que el llamador inicie un logout limpio (fuerza re-login).
            invalidateKey(alias)
            null
        } catch (e: Exception) {
            // Cualquier otro fallo (KeyStore corrupto, datos mal formados, ...).
            invalidateKey(alias)
            null
        }
    }

    /**
     * Obtiene la clave del Keystore o la crea la primera vez.
     * Sincronizado porque generar la misma clave desde dos hilos provoca KeyStoreException.
     */
    @Synchronized
    private fun getOrCreateKey(alias: String): SecretKey {
        getKey(alias)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // setRandomizedEncryptionRequired(true) es el default: IV aleatorio por cifrado.
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun getKey(alias: String): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.getKey(alias, null) as? SecretKey
        } catch (e: Exception) {
            null
        }
    }

    private fun invalidateKey(alias: String) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        } catch (ignored: Exception) {
            // Si no se puede borrar, el siguiente ciclo de cifrado/descifrado fallará
            // de nuevo; la app seguirá funcionando sin sesión.
        }
    }

    companion object {
        /** Alias único de la clave maestro dentro del AndroidKeyStore. */
        const val DEFAULT_KEY_ALIAS = "sifa_keystore_master_key"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }
}