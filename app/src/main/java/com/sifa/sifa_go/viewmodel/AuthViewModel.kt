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
import com.sifa.sifa_go.exception.NetworkErrorHandler
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

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null) {
                        sessionManager.saveSession(
                            token = body.accessToken,
                            username = body.sub
                        )
                        isLoginSuccessful = true
                    } else {
                        loginError = "Respuesta vacía del servidor"
                    }

                } else {
                    loginError = NetworkErrorHandler.getErrorMessage(response.code())
                }

            } catch (e: Exception) {
                loginError = NetworkErrorHandler.getExceptionMessage(e)
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