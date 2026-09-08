package com.sifa.sifa_go.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sifa.sifa_go.core.security.KeystoreAesCipher
import com.sifa.sifa_go.core.security.SecureCipher
import com.sifa.sifa_go.core.security.StorageMigration
import com.sifa.sifa_go.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Gestor de la sesión del usuario.
 *
 * Almacenamiento SEGURO:
 *  - Los tokens se cifran con AES/GCM respaldado por una clave maestro del
 *    **Android Keystore** (ver [KeystoreAesCipher]).
 *  - En disco solo existe ciphertext Base64; el master key nunca sale del hardware.
 *  - Los archivos de prefs sensibles se excluyen del backup en `backup_rules.xml`
 *    y `data_extraction_rules.xml` (defensa en profundidad).
 *  - El constructor `SessionManager(context)` ejecuta la migración one-time desde
 *    el archivo legacy en texto plano (`sifa_session`) y lo elimina del disco.
 */
class SessionManager(
    private val prefs: SharedPreferences,
    private val cipher: SecureCipher,
    private val keyAlias: String,
) : SessionRepository {

    /**
     * Constructor de producción. Mantiene la firma original `SessionManager(context)`
     * para no tocar los ~8 puntos de uso existentes.
     */
    constructor(context: Context) : this(
        prefs = context.getSharedPreferences(PREFS_NAME_ENC, Context.MODE_PRIVATE),
        cipher = KeystoreAesCipher(),
        keyAlias = KeystoreAesCipher.DEFAULT_KEY_ALIAS,
    ) {
        // Migración one-time: lectura texto plano legacy -> cifrado -> borrado del legacy.
        StorageMigration.migrateLegacyToEncrypted(
            context = context,
            legacyPrefsName = LEGACY_PREFS_NAME,
            targetPrefs = prefs,
            cipher = cipher,
            keyAlias = keyAlias,
        )
    }

    companion object {
        // Nombre del nuevo archivo con valores cifrados (se excluye del backup).
        private const val PREFS_NAME_ENC = "sifa_session_enc"
        // Nombre del archivo legacy en texto plano que se migra y luego se borra.
        private const val LEGACY_PREFS_NAME = "sifa_session"

        private const val KEY_TOKEN = "TOKEN"
        private const val KEY_REFRESH_TOKEN = "REFRESH_TOKEN"
        private const val KEY_TOKEN_EXPIRY = "TOKEN_EXPIRY"
        private const val KEY_TOKEN_IAT = "TOKEN_IAT"
        private const val KEY_USERNAME = "USERNAME"
        private const val KEY_ROLES = "ROLES"

        private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

        fun notifySessionExpired() {
            _sessionExpiredEvent.tryEmit(Unit)
        }
    }

    override fun saveSession(
        token: String,
        refreshToken: String,
        username: String,
        roles: List<String>,
        expiry: Long?,
        iat: Long?
    ) {
        // ROLES se persistía como StringSet; ahora se serializa en un único string
        // cifrado (separado por salto de línea) para simplificar el formato cifrado.
        prefs.edit().apply {
            putString(KEY_TOKEN, cipher.encrypt(keyAlias, token))
            putString(KEY_REFRESH_TOKEN, cipher.encrypt(keyAlias, refreshToken))
            putString(KEY_USERNAME, cipher.encrypt(keyAlias, username))
            putString(KEY_ROLES, cipher.encrypt(keyAlias, roles.joinToString("\n")))
            if (expiry != null) putString(KEY_TOKEN_EXPIRY, cipher.encrypt(keyAlias, expiry.toString()))
            if (iat != null) putString(KEY_TOKEN_IAT, cipher.encrypt(keyAlias, iat.toString()))
            apply() // apply() guarda de forma asíncrona (más rápido)
        }
    }

    override fun getToken(): String? = decryptString(KEY_TOKEN)

    override fun getRefreshToken(): String? = decryptString(KEY_REFRESH_TOKEN)

    override fun getTokenExpiry(): Long = decryptString(KEY_TOKEN_EXPIRY)?.toLongOrNull() ?: 0L

    override fun getTokenIat(): Long = decryptString(KEY_TOKEN_IAT)?.toLongOrNull() ?: 0L

    override fun getUsername(): String? = decryptString(KEY_USERNAME)

    override fun getRoles(): List<String> =
        decryptString(KEY_ROLES)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    override fun hasUserAppRole(): Boolean {
        return getRoles().contains("USER_APP")
    }

    override fun hasValidSession(): Boolean {
        return !getToken().isNullOrEmpty() && !getRefreshToken().isNullOrEmpty() && hasUserAppRole()
    }

    override fun logout() {
        // clear() borraría también la flag de migración; la volvemos a marcar para
        // que la migración no se reintente (ya no existe archivo legacy).
        prefs.edit {
            clear()
            putBoolean(StorageMigration.MIGRATED_FLAG, true)
        }
    }

    /**
     * Lee el valor cifrado del prefs y lo descifra.
     * Devuelve `null` si no existe o si la clave del Keystore fue invalidada/corrupta
     * (en ese caso [KeystoreAesCipher] ya invalidó la clave y el flujo de auth
     * terminará en logout limpio).
     */
    private fun decryptString(key: String): String? {
        val raw = prefs.getString(key, null) ?: return null
        return cipher.decrypt(keyAlias, raw)
    }
}