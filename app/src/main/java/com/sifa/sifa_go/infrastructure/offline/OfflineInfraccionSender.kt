package com.sifa.sifa_go.infrastructure.offline

import android.util.Log
import com.google.gson.Gson
import com.sifa.sifa_go.core.image.toCleanMultipartPart
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

/**
 * Resultado del envío de una infracción encolada.
 * [failureReason] solo se completa cuando el resultado no es [SyncResult.Success],
 * para permitir diagnosticar por qué falló.
 */
sealed class SyncResult(val failureReason: String? = null) {
    data object Success : SyncResult()

    /** La infracción se reenviará cuando vuelva la conexión. */
    data class TransientNetworkError(val reason: String) : SyncResult(reason)

    /** La infracción no se puede reenviar (ej: faltan fotos o error de validación del backend). Debe marcarse FAILED. */
    data class PermanentError(val reason: String) : SyncResult(reason)
}

/**
 * Encapsula la lógica de envío de una infracción pendiente al backend.
 * Compartida entre [OfflineSyncCoordinator] (foreground) y [OfflineSyncWorker] (background).
 */
object OfflineInfraccionSender {

    private const val TAG = "OfflineSender"
    private val gson = Gson()

    /**
     * Reconstruye el multipart desde el request tipado y las rutas de fotos,
     * replicando exactamente lo que hace `CoreApiService.createInfraccion`.
     */
    suspend fun send(
        request: InfraccionCreateRequest,
        imagePaths: List<String>
    ): SyncResult {
        // Si alguna foto de evidencia ya no existe, no podemos rearmar el request.
        if (imagePaths.isNotEmpty()) {
            val missing = imagePaths.filterNot { File(it).exists() }
            if (missing.isNotEmpty()) {
                val reason = "Faltan ${missing.size} foto(s) de evidencia: ${missing.joinToString()}"
                Log.e(TAG, "PermanentError -> $reason")
                return SyncResult.PermanentError(reason)
            }
        }

        return try {
            val jsonRequest = gson.toJson(request)
                .toRequestBody("application/json".toMediaTypeOrNull())

            val fotoParts = imagePaths.map { path ->
                File(path).toCleanMultipartPart("fotos")
            }

            val response = CoreRetrofitClient.apiService.createInfraccion(
                request = jsonRequest,
                fotos = fotoParts
            )

            // createInfraccion devuelve InfraccionResponse (NO una Response<...>),
            // por lo que Retrofit lanza HttpException en códigos 4xx/5xx.
            Log.i(TAG, "Infracción enviada: $response")
            SyncResult.Success
        } catch (e: HttpException) {
            // Error del backend (4xx/5xx). Es permanente: reintentar no ayudará.
            val reason = "Backend respondió HTTP ${e.code()}: ${e.message()}"
            Log.e(TAG, "PermanentError -> $reason", e)
            SyncResult.PermanentError(reason)
        } catch (e: IOException) {
            // Sin red o timeout: transitorio, se reintentará al reconectar.
            val reason = "Error de red: ${e.javaClass.simpleName}: ${e.message}"
            Log.w(TAG, "TransientNetworkError -> $reason", e)
            SyncResult.TransientNetworkError(reason)
        } catch (e: Exception) {
            // Cualquier otra cosa (parseo, serialización, etc.). Transitorio por ahora.
            val reason = "Error inesperado: ${e.javaClass.simpleName}: ${e.message}"
            Log.e(TAG, "TransientNetworkError -> $reason", e)
            SyncResult.TransientNetworkError(reason)
        }
    }
}
