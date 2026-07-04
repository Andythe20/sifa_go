package com.sifa.sifa_go.core.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    private const val TAG = "BiometricHelper"
    const val REQUEST_CODE_DEVICE_CREDENTIAL = 0xB103

    private var pendingSuccess: (() -> Unit)? = null
    private var pendingError: ((String) -> Unit)? = null

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = context as? FragmentActivity ?: run {
            onError("Error interno: El contexto no soporta autenticación.")
            return
        }

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                authenticateModern(activity, onSuccess, onError)
            }
            else -> {
                authenticateLegacy(activity, onSuccess, onError)
            }
        }
    }

    private fun authenticateModern(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val mgr = BiometricManager.from(activity)
        val canAuthenticate = try {
            mgr.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Error checking biometric availability", e)
            false
        }

        if (!canAuthenticate) {
            val canCredential = try {
                mgr.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            } catch (_: Exception) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
            }
            if (canCredential == BiometricManager.BIOMETRIC_SUCCESS) {
                showDeviceCredentialPrompt(activity, onSuccess, onError)
                return
            }
            onError("No hay método de autenticación disponible en el dispositivo.")
            return
        }

        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString.toString())
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }
                }
            )
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Acceso Seguro SIFA GO")
                    .setSubtitle("Usa tu huella para acceder como fiscalizador")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Biometric authentication failed", e)
            onError("Error al iniciar autenticación biométrica.")
        }
    }

    @Suppress("DEPRECATION")
    private fun authenticateLegacy(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            onError("No hay PIN, patrón o contraseña configurados en el dispositivo.")
            return
        }

        try {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Acceso Seguro SIFA GO",
                "Usa tu huella o ingresa tu PIN para acceder como fiscalizador"
            )
            if (intent != null) {
                pendingSuccess = onSuccess
                pendingError = onError
                activity.startActivityForResult(intent, REQUEST_CODE_DEVICE_CREDENTIAL)
            } else {
                onError("No se pudo iniciar la autenticación.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Device credential prompt failed", e)
            onError("Error al iniciar autenticación por credenciales.")
        }
    }

    @Suppress("DEPRECATION")
    private fun showDeviceCredentialPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            onError("No hay PIN, patrón o contraseña configurados en el dispositivo.")
            return
        }

        try {
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                "Acceso Seguro SIFA GO",
                "Ingresa tu PIN, patrón o contraseña para acceder"
            )
            if (intent != null) {
                pendingSuccess = onSuccess
                pendingError = onError
                activity.startActivityForResult(intent, REQUEST_CODE_DEVICE_CREDENTIAL)
            } else {
                onError("No se pudo iniciar la autenticación.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Device credential prompt failed", e)
            onError("Error al iniciar autenticación por credenciales.")
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_DEVICE_CREDENTIAL) {
            if (resultCode == FragmentActivity.RESULT_OK) {
                pendingSuccess?.invoke()
            } else {
                pendingError?.invoke("Autenticación cancelada.")
            }
            pendingSuccess = null
            pendingError = null
        }
    }
}
