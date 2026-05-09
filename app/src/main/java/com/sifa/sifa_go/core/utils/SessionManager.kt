package com.sifa.sifa_go.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {

    // Usamos SharedPreferences para almacenar datos de sesión de forma persistente
    companion object {
        // El nombre del archivo de preferencias
        private const val PREFS_NAME = "sifa_session"

        // Las claves para almacenar los datos de sesión
        private const val KEY_TOKEN = "TOKEN"
        private const val KEY_USERNAME = "USERNAME"
        private const val KEY_ROLES = "ROLES"
    }

    // Instancia de SharedPreferences
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    // Guardar los datos de sesión
    fun saveSession(
        token: String,
        username: String,
        roles: List<String>
    ) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USERNAME, username)
            putStringSet(KEY_ROLES, roles.toSet())
            apply() // apply() guarda de forma asíncrona (más rápido)
        }
    }

    // Obtener datos
    fun getToken(): String? =
        prefs.getString(KEY_TOKEN, null)

    fun getUsername(): String? =
        prefs.getString(KEY_USERNAME, null)

    fun getRoles(): List<String> =
        prefs.getStringSet(KEY_ROLES, emptySet())
            ?.toList()
            ?: emptyList()

    // Validar rol USER_APP
    fun hasUserAppRole(): Boolean {
        return getRoles().contains("USER_APP")
    }

    // Validar si la sesión es válida (token no nulo y rol USER_APP presente)
    fun hasValidSession(): Boolean {
        return !getToken().isNullOrEmpty() && hasUserAppRole()
    }

    // Borrar sesión (Para el botón de Cerrar Sesión)
    fun logout() {
        prefs.edit { clear() }
    }
}