package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.core.preferences.AppPreferences

/**
 * Acá definimos la configuración del servidor, como la URL base y los tiempos de timeout para las solicitudes.
 */
object ServerConfig {
    val BASE_URL: String
        get() = AppPreferences.baseUrl
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L
    const val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L
    const val SESSION_EXPIRED_DELAY_MS = 3_500L // Duración del spinner
    const val VIBRATE_SHORT_MS = 70L //Duración de la vibración
    const val VIBRATE_SUCCESS_DELAY_MS = 150L // Pausa antes de navegar hacia la siguiente vista
}
