package com.example.sifa_go.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SessionManager(context: Context) {
    // Archivo de preferencias privado para la app
    private val prefs: SharedPreferences = context.getSharedPreferences("sifa_session", Context.MODE_PRIVATE)

    // Guardar los datos de sesión
    fun saveSession(token: String, username: String) {
        prefs.edit().apply {
            putString("TOKEN", token)
            putString("USERNAME", username)
            apply() // apply() guarda de forma asíncrona (más rápido)
        }
    }

    // Recuperar datos
    fun getToken(): String? = prefs.getString("TOKEN", null)
    fun getUsername(): String? = prefs.getString("USERNAME", null)

    // Borrar sesión (Para el botón de Cerrar Sesión)
    fun logout() {
        prefs.edit { clear() }
    }
}