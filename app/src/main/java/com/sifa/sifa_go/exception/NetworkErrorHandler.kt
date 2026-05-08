package com.sifa.sifa_go.exception

object NetworkErrorHandler {

    fun getErrorMessage(code: Int): String {
        return when (code) {
            400 -> "Solicitud inválida. Verifica los datos ingresados."
            401 -> "Credenciales incorrectas."
            403 -> "Acceso denegado."
            404 -> "Servicio no encontrado."
            408, 504 -> "Tiempo de espera agotado."
            409 -> "Conflicto en la solicitud."
            500 -> "Error del servidor."
            else -> "Error inesperado ($code)." // TODO: Dejar un mensaje genérico para errores no manejados"
        }
    }

    fun getExceptionMessage(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException ->
                "Sin conexión a internet."

            is java.net.SocketTimeoutException ->
                "Tiempo de conexión agotado."

            else ->
                "Error de conexión. Intenta nuevamente."
        }
    }
}