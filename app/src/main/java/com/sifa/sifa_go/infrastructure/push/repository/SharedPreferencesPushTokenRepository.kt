package com.sifa.sifa_go.infrastructure.push.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sifa.sifa_go.domain.push.PushToken
import com.sifa.sifa_go.domain.push.PushTokenRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SharedPreferencesPushTokenRepository(context: Context) : PushTokenRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getToken(): PushToken? {
        val token = prefs.getString(KEY_FCM_TOKEN, null) ?: return null
        return PushToken(token)
    }

    override suspend fun saveToken(token: PushToken) {
        prefs.edit { putString(KEY_FCM_TOKEN, token.value) }
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
        private const val PREFS_NAME = "sifa_push"
        private const val KEY_FCM_TOKEN = "FCM_TOKEN"
    }
}
