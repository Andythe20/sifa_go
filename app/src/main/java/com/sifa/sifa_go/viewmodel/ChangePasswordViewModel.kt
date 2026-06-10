package com.sifa.sifa_go.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.AuthRetrofitClient
import com.sifa.sifa_go.core.network.NetworkModule
import com.sifa.sifa_go.core.utils.PasswordValidator
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.ChangePasswordRequest
import com.sifa.sifa_go.exception.NetworkErrorHandler
import kotlinx.coroutines.launch

class ChangePasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    var oldPassword by mutableStateOf("")
        private set

    var newPassword by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var acceptLogout by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isSuccess by mutableStateOf(false)
        private set

    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    init {
        NetworkModule.init(application)
    }

    val passwordRequirements: List<PasswordValidator.Requirement>
        get() = PasswordValidator.validate(newPassword)

    val isNewPasswordValid: Boolean
        get() = PasswordValidator.isFullyValid(newPassword)

    val passwordsMatch: Boolean
        get() = confirmPassword.isEmpty() || newPassword == confirmPassword

    val canSubmit: Boolean
        get() = oldPassword.isNotBlank()
                && isNewPasswordValid
                && passwordsMatch
                && newPassword == confirmPassword
                && !isLoading

    fun onOldPasswordChanged(value: String) {
        oldPassword = value
        fieldErrors = fieldErrors - "oldPassword"
        error = null
    }

    fun onNewPasswordChanged(value: String) {
        newPassword = value
        fieldErrors = fieldErrors - "newPassword"
        error = null
    }

    fun onConfirmPasswordChanged(value: String) {
        confirmPassword = value
        fieldErrors = fieldErrors - "confirmPassword"
        error = null
    }

    fun onAcceptLogoutChanged(value: Boolean) {
        acceptLogout = value
    }

    fun submit() {
        fieldErrors = emptyMap()
        error = null

        if (oldPassword.isBlank()) {
            fieldErrors = fieldErrors + ("oldPassword" to "Ingresa tu contraseña actual")
            return
        }

        if (!isNewPasswordValid) {
            fieldErrors = fieldErrors + ("newPassword" to "La nueva contraseña no cumple los requisitos")
            return
        }

        if (newPassword.length < 8) {
            fieldErrors = fieldErrors + ("newPassword" to "Debe tener al menos 8 caracteres")
            return
        }

        if (newPassword != confirmPassword) {
            fieldErrors = fieldErrors + ("confirmPassword" to "Las contraseñas no coinciden")
            return
        }

        if (oldPassword == newPassword) {
            fieldErrors = fieldErrors + ("newPassword" to "La nueva contraseña debe ser diferente a la actual")
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            try {
                val request = ChangePasswordRequest(
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )

                Log.d("ChangePassVM", "Enviando cambio de contraseña")
                val response = AuthRetrofitClient.apiService.changePassword(request)

                if (response.isSuccessful) {
                    Log.d("ChangePassVM", "Cambio de contraseña exitoso")
                    isSuccess = true
                    sessionManager.logout()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.d("ChangePassVM", "Error: ${response.code()} - $errorBody")

                    error = when {
                        response.code() == 400 && errorBody?.contains("actual no es correcta") == true ->
                            "La contraseña actual no es correcta"
                        response.code() == 400 && errorBody?.contains("diferente a la actual") == true ->
                            "La nueva contraseña debe ser diferente a la actual"
                        response.code() == 401 ->
                            "Tu sesión ha expirado. Vuelve a iniciar sesión."
                        else ->
                            NetworkErrorHandler.getErrorMessage(response.code())
                    }
                }
            } catch (e: Exception) {
                Log.e("ChangePassVM", "Excepción en cambio de contraseña", e)
                error = NetworkErrorHandler.getExceptionMessage(e)
            } finally {
                isLoading = false
            }
        }
    }
}
