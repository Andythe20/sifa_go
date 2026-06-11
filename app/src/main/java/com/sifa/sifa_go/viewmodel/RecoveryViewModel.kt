package com.sifa.sifa_go.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.AuthRetrofitClient
import com.sifa.sifa_go.core.network.NetworkModule
import com.sifa.sifa_go.core.utils.PasswordValidator
import com.sifa.sifa_go.data.model.PasswordRecoveryRequest
import com.sifa.sifa_go.data.model.PasswordResetRequest
import com.sifa.sifa_go.exception.NetworkErrorHandler
import kotlinx.coroutines.launch

class RecoveryViewModel(application: Application) : AndroidViewModel(application) {

    var step by mutableStateOf(1)
        private set

    var email by mutableStateOf("")
        private set

    var code by mutableStateOf("")
        private set

    var newPassword by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var fieldErrors by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    var recoveryRequestedAt by mutableStateOf<Long?>(null)
        private set

    init {
        NetworkModule.init(application)
        // Skip step 1 — start at the code + password step for development //TODO
//        email = "dev@test.com" // TODO
//        recoveryRequestedAt = System.currentTimeMillis() //TODO
//        step = 2 //TODO
    }

    val passwordRequirements: List<PasswordValidator.Requirement>
        get() = PasswordValidator.validate(newPassword)

    val isNewPasswordValid: Boolean
        get() = PasswordValidator.isFullyValid(newPassword)

    val passwordsMatch: Boolean
        get() = confirmPassword.isEmpty() || newPassword == confirmPassword

    fun onEmailChanged(value: String) {
        email = value
        fieldErrors = fieldErrors - "email"
        error = null
    }

    fun onCodeChanged(value: String) {
        if (value.all { it.isDigit() } && value.length <= 6) {
            code = value
            fieldErrors = fieldErrors - "code"
            error = null
        }
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

    fun requestCode() {
        fieldErrors = emptyMap()
        error = null

        if (email.isBlank()) {
            fieldErrors = fieldErrors + ("email" to "El correo es obligatorio")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            fieldErrors = fieldErrors + ("email" to "Ingresa un formato de correo válido")
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            try {
                val request = PasswordRecoveryRequest(email = email.trim())
                val response = AuthRetrofitClient.apiService.requestRecovery(request)

                if (response.isSuccessful) {
                    code = ""
                    newPassword = ""
                    confirmPassword = ""
                    fieldErrors = emptyMap()
                    recoveryRequestedAt = System.currentTimeMillis()
                    step = 2
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = if (errorBody?.contains("El correo ingresado no se encuentra registrado") == true) {
                        "El correo ingresado no se encuentra registrado"
                    } else if (errorBody?.contains("inactiva") == true) {
                        "Esta cuenta se encuentra inactiva. Contacte al administrador."
                    } else {
                        NetworkErrorHandler.getErrorMessage(response.code())
                    }
                    error = message
                }
            } catch (e: Exception) {
                error = NetworkErrorHandler.getExceptionMessage(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun resetPassword() {
        fieldErrors = emptyMap()
        error = null

        val errors = mutableMapOf<String, String>()

        if (code.length != 6) {
            errors["code"] = "El código debe tener exactamente 6 dígitos"
        }

        if (!isNewPasswordValid) {
            errors["newPassword"] = "La nueva contraseña no cumple los requisitos"
        }

        if (newPassword != confirmPassword) {
            errors["confirmPassword"] = "Las contraseñas no coinciden"
        }

        if (errors.isNotEmpty()) {
            fieldErrors = errors
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            try {
                val request = PasswordResetRequest(
                    email = email.trim(),
                    code = code.trim(),
                    newPassword = newPassword
                )
                val response = AuthRetrofitClient.apiService.resetPassword(request)

                if (response.isSuccessful) {
                    step = 3
                } else {
                    val errorBody = response.errorBody()?.string()
                    val message = when {
                        errorBody?.contains("expirado") == true ->
                            "El código de recuperación ha expirado. Solicita uno nuevo."
                        errorBody?.contains("bloqueado") == true ->
                            "Código bloqueado por superar el límite de intentos. Solicita uno nuevo."
                        errorBody?.contains("incorrecto") == true ->
                            parseRemainingAttempts(errorBody)
                        errorBody?.contains("inactiva") == true ->
                            "Esta cuenta se encuentra inactiva. Contacte al administrador."
                        else ->
                            NetworkErrorHandler.getErrorMessage(response.code())
                    }
                    error = message
                }
            } catch (e: Exception) {
                error = NetworkErrorHandler.getExceptionMessage(e)
            } finally {
                isLoading = false
            }
        }
    }

    fun goToStep1() {
        code = ""
        newPassword = ""
        confirmPassword = ""
        fieldErrors = emptyMap()
        error = null
        recoveryRequestedAt = null
        step = 1
    }

    private fun parseRemainingAttempts(errorBody: String): String {
        val remainingMatch = Regex("""Intentos restantes: (\d+)""").find(errorBody)
        return if (remainingMatch != null) {
            val remaining = remainingMatch.groupValues[1]
            "El código ingresado es incorrecto. Intentos restantes: $remaining"
        } else {
            "El código ingresado es incorrecto."
        }
    }
}
