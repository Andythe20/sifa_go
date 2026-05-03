package com.sifa.sifa_go.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.AuthRetrofitClient
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.LoginRequest
import kotlinx.coroutines.launch

// Usamos AndroidViewModel para poder tener acceso al Contexto de la app y usar SharedPreferences
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    var isLoading by mutableStateOf(false)
    var loginError by mutableStateOf<String?>(null)
    var isLoginSuccessful by mutableStateOf(false)

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true
            loginError = null
            try {
                // Creamos la petición JSON
                val request = LoginRequest(username = email, password = pass)
                // Llamamos a Python
                val response = AuthRetrofitClient.apiService.login(request)

                // ¡ÉXITO! Guardamos el token y el username en el celular
                sessionManager.saveSession(token = response.token, username = response.username)

                // Disparamos la navegación
                isLoginSuccessful = true

            } catch (e: Exception) {
                // Si la credencial es incorrecta o falla la red
                loginError = "Credenciales incorrectas o error de red"
                println(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun loginWithBiometrics() {
        // Revisamos si el usuario ya tiene una sesión iniciada y guardada
        val savedToken = sessionManager.getToken()

        if (savedToken != null) {
            // Si hay token, lo dejamos entrar directamente
            isLoginSuccessful = true
        } else {
            // Si no hay token, es la primera vez que usa la app. Debe usar contraseña.
            loginError = "Primera vez: Por favor inicia sesión con correo y contraseña primero."
        }
    }
}