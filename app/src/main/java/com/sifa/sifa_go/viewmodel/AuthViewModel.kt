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
                val request = LoginRequest(email = email, password = pass)
                val response = AuthRetrofitClient.apiService.login(request)

                when (response.code()) {
                    200 -> {
                        val body = response.body()!!
                        sessionManager.saveSession(token = body.accessToken, username = body.sub)
                        isLoginSuccessful = true
                    }
                    400 -> {
                        loginError = "Solicitud inválida. Verifica los datos ingresados."
                    }
                    401 -> {
                        loginError = "Credenciales incorrectas. Verifica tu email y contraseña."
                    }
                    403 -> {
                        loginError = "Acceso denegado. Tu cuenta ha sido suspendida o bloqueada."
                    }
                    404 -> {
                        loginError = "Servicio no encontrado. Contacta al administrador."
                    }
                    408, 504 -> {
                        loginError = "Tiempo de espera agotado. Verifica tu conexión a internet."
                    }
                    409 -> {
                        loginError = "Conflicto en la solicitud. Intenta nuevamente."
                    }
                    500 -> {
                        loginError = "Error del servidor. Intenta más tarde."
                    }
                    else -> {
                        loginError = "Error inesperado (${response.code()}). Intenta nuevamente."
                    }
                }

            } catch (e: Exception) {
                loginError = when {
                    e.message?.contains("Unable to resolve host") == true -> "Sin conexión a internet. Verifica tu red."
                    e.message?.contains("timeout") == true -> "Tiempo de conexión agotado. Intenta nuevamente."
                    else -> "Error: " + e.message // TODO: Quitar detalles técnicos en producción y mostrar mensaje genérico "Error de conexión. Intenta nuevamente."
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun loginWithBiometrics() {
        val savedToken = sessionManager.getToken()

        if (savedToken != null) {
            isLoginSuccessful = true
        } else {
            loginError = "Primera vez: Por favor inicia sesión con correo y contraseña primero."
        }
    }
}