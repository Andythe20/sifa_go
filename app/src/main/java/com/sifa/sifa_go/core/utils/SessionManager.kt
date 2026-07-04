package com.sifa.sifa_go.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sifa.sifa_go.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SessionManager(context: Context) : SessionRepository {

    companion object {
        private const val PREFS_NAME = "sifa_session"

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

    // Instancia de SharedPreferences
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    override fun saveSession(
        token: String,
        refreshToken: String,
        username: String,
        roles: List<String>,
        expiry: Long?,
        iat: Long?
    ) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USERNAME, username)
            putStringSet(KEY_ROLES, roles.toSet())
            if (expiry != null) putLong(KEY_TOKEN_EXPIRY, expiry)
            if (iat != null) putLong(KEY_TOKEN_IAT, iat)
            apply() // apply() guarda de forma asíncrona (más rápido)
        }
    }

    override fun getToken(): String? =
        prefs.getString(KEY_TOKEN, null)

    override fun getRefreshToken(): String? =
        prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun getTokenExpiry(): Long =
        prefs.getLong(KEY_TOKEN_EXPIRY, 0L)

    override fun getTokenIat(): Long =
        prefs.getLong(KEY_TOKEN_IAT, 0L)

    override fun getUsername(): String? =
        prefs.getString(KEY_USERNAME, null)

    override fun getRoles(): List<String> =
        prefs.getStringSet(KEY_ROLES, emptySet())
            ?.toList()
            ?: emptyList()

    override fun hasUserAppRole(): Boolean {
        return getRoles().contains("USER_APP")
    }

    override fun hasValidSession(): Boolean {
        return !getToken().isNullOrEmpty() && !getRefreshToken().isNullOrEmpty() && hasUserAppRole()
    }

    override fun logout() {
        prefs.edit { clear() }
    }
}