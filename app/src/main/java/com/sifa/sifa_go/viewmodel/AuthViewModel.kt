package com.sifa.sifa_go.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.AuthRetrofitClient
import com.sifa.sifa_go.core.network.DeviceRetrofitClient
import com.sifa.sifa_go.core.network.NetworkModule
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.LoginRequest
import com.sifa.sifa_go.domain.push.usecase.RegisterDeviceUseCase
import com.sifa.sifa_go.exception.NetworkErrorHandler
import com.sifa.sifa_go.infrastructure.push.repository.SharedPreferencesPushTokenRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val registerDeviceUseCase = RegisterDeviceUseCase(
        tokenRepository = SharedPreferencesPushTokenRepository(application),
        deviceApi = DeviceRetrofitClient.apiService
    )

    init {
        NetworkModule.init(application)
    }

    var isLoading by mutableStateOf(false)
    var loginError by mutableStateOf<String?>(null)
    var isLoginSuccessful by mutableStateOf(false)

    fun login(email: String, pass: String) {

        viewModelScope.launch {

            isLoading = true
            loginError = null

            try {
                val request = LoginRequest(
                    email = email,
                    password = pass
                )

                val response = AuthRetrofitClient.apiService.login(request)

                // VALIDAR RESPUESTA
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // VALIDAR ROL
                        if (body.roles.contains("USER_APP")) {
                            // GUARDAR SESIÓN
                            sessionManager.saveSession(
                                token = body.accessToken,
                                refreshToken = body.refreshToken,
                                username = body.sub,
                                roles = body.roles,
                                expiry = body.exp,
                                iat = body.iat
                            )
                            isLoginSuccessful = true
                            registerDevice()
                        } else {
                            loginError = "No tienes permisos para acceder a esta aplicación"
                            sessionManager.logout()
                        }
                    } else {
                        loginError = "Respuesta vacía del servidor"
                    }
                } else {
                    loginError = NetworkErrorHandler.getErrorMessage(response.code())
                }
            } catch (e: Exception) {
                loginError = NetworkErrorHandler.getExceptionMessage(e)
            } finally {
                if (loginError != null) isLoading = false
            }
        }
    }

    fun loginWithBiometrics() {
        if (sessionManager.hasValidSession()) {
            isLoginSuccessful = true
            registerDevice()
        } else {
            loginError =
                "No se encontró una sesión válida. Por favor, inicia sesión con tus credenciales primero."
            sessionManager.logout()
        }
    }

    private fun registerDevice() {
        viewModelScope.launch {
            registerDeviceUseCase()
                .onSuccess { Log.d(TAG, "Device registered successfully") }
                .onFailure { Log.w(TAG, "Device registration failed: ${it.message}") }
        }
    }

    companion object {
        private const val TAG = "AuthVM"
    }
}