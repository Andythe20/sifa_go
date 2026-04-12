package com.example.sifa_go.core.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Verificamos que el contexto sea compatible
        val fragmentActivity = context as? FragmentActivity ?: run {
            onError("Error interno: El contexto no soporta biometría.")
            return
        }

        // Ejecutor que corre en el hilo principal
        val executor = ContextCompat.getMainExecutor(context)

        // Configuramos qué pasa cuando la huella es correcta o falla
        val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // ¡Huella correcta!
                    onSuccess()
                }
            }
        )

        // Diseñamos el cuadro de diálogo que le aparece al usuario
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Seguro SIFA GO")
            .setSubtitle("Usa tu huella para acceder como fiscalizador")
            // Permite usar huella, rostro o el PIN/Patrón del celular
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        // Lanzamos el diálogo
        biometricPrompt.authenticate(promptInfo)
    }
}