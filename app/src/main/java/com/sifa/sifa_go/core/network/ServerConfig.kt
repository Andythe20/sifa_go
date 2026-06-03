package com.sifa.sifa_go.core.network

/**
 * Acá definimos la configuración del servidor, como la URL base y los tiempos de timeout para las solicitudes.
 */
object ServerConfig {
    const val BASE_URL = "http://3.219.255.24"
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L
    const val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L
}
