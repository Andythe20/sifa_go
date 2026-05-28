package com.sifa.sifa_go.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.AuthRetrofitClient
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.UserResponse
import com.sifa.sifa_go.exception.NetworkErrorHandler
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    var user by mutableStateOf<UserResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val email = sessionManager.getUsername()

        if (email.isNullOrEmpty()) {
            error = "No se encontró el email del usuario"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            try {
                val response = AuthRetrofitClient.apiService.getUserByEmail(email)

                if (response.isSuccessful) {
                    user = response.body()
                } else {
                    error = NetworkErrorHandler.getErrorMessage(response.code())
                }
            } catch (e: Exception) {
                error = NetworkErrorHandler.getExceptionMessage(e)
            } finally {
                isLoading = false
            }
        }
    }
}