package com.sifa.sifa_go.infrastructure.push.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sifa.sifa_go.core.security.KeystoreAesCipher
import com.sifa.sifa_go.core.security.SecureCipher
import com.sifa.sifa_go.core.security.StorageMigration
import com.sifa.sifa_go.domain.push.PushToken
import com.sifa.sifa_go.domain.push.PushTokenRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repositorio del FCM token con almacenamiento cifrado vía Android Keystore.
 *
 * El token se almacena como ciphertext Base64 (AES/GCM) en el archivo
 * `sifa_push_enc`. En el arranque se ejecuta la migración one-time desde el
 * archivo legacy en texto plano `sifa_push`, que luego se elimina del disco.
 */
class SharedPreferencesPushTokenRepository(context: Context) : PushTokenRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME_ENC, Context.MODE_PRIVATE)

    private val cipher: SecureCipher = KeystoreAesCipher()

    init {
        // Migración one-time: texto plano legacy -> cifrado -> borrado del legacy.
        StorageMigration.migrateLegacyToEncrypted(
            context = context,
            legacyPrefsName = LEGACY_PREFS_NAME,
            targetPrefs = prefs,
            cipher = cipher,
            keyAlias = KeystoreAesCipher.DEFAULT_KEY_ALIAS,
        )
    }

    override fun getToken(): PushToken? {
        val raw = prefs.getString(KEY_FCM_TOKEN, null) ?: return null
        val token = cipher.decrypt(KeystoreAesCipher.DEFAULT_KEY_ALIAS, raw) ?: return null
        return PushToken(token)
    }

    override suspend fun saveToken(token: PushToken) {
        val encrypted = cipher.encrypt(KeystoreAesCipher.DEFAULT_KEY_ALIAS, token.value)
        prefs.edit { putString(KEY_FCM_TOKEN, encrypted) }
    }

    override fun observeToken(): Flow<PushToken?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FCM_TOKEN) {
                trySend(getToken())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getToken())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    companion object {
        // Nuevo archivo con valores cifrados (se excluye del backup).
        private const val PREFS_NAME_ENC = "sifa_push_enc"
        // Archivo legacy en texto plano que se migra y luego se borra.
        private const val LEGACY_PREFS_NAME = "sifa_push"

        private const val KEY_FCM_TOKEN = "FCM_TOKEN"
    }
}