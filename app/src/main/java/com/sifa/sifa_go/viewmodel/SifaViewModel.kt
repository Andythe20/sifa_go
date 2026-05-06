package com.sifa.sifa_go.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.RetrofitClient
import com.sifa.sifa_go.core.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SifaViewModel(application: Application) : AndroidViewModel(application) {

    // Instanciamos el manejador de sesión
    private val sessionManager = SessionManager(application)

    // Variable que guardará la ruta de la foto de forma global
    // Usamos mutableStateOf para que la interfaz se actualice si esto cambia
    var currentPhotoPath by mutableStateOf<String?>(null)

    // variables para manejar el estado de la API
    var isLoading by mutableStateOf(false)
    var detectedPlate by mutableStateOf<String?>(null)
    var detectionError by mutableStateOf<String?>(null)

    // Función para limpiar los datos cuando se termine una multa o se cancele
    fun clearProcess() {
        currentPhotoPath = null
        detectedPlate = null
        detectionError = null
    }

    // Función para enviar la imagen
    fun uploadImageToBackend(filePath: String) {
        viewModelScope.launch {
            isLoading = true
            // Limpiamos estados anteriores
            detectedPlate = null
            detectionError = null
            try {
                // Obtenemos el token guardado en el celular
                val token = sessionManager.getToken() ?: ""

                val file = File(filePath)

                // Preparamos el archivo para enviarlo por HTTP
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // Hacemos la llamada a la API incluyendo el token
                val response = RetrofitClient.apiService.detectPlate(
                    token = "Bearer $token",
                    file = body
                )

                // Verificamos si la IA encontró una patente en la foto
                val plateResult = response.result.firstOrNull()

                if (plateResult != null && plateResult.success && !plateResult.plate.isNullOrEmpty()) {
                    // IA exitosa
                    detectedPlate = plateResult.plate
                } else {
                    // La IA respondió, pero no encontró ninguna patente legible en la foto
                    detectionError = "No se logró leer la patente en la fotografía."
                }

            } catch (e: Exception) {
                // Falla de red, PC apagado, timeout, etc.
                detectionError = "Error de conexión con el motor de IA. Comprueba tu red."
                println(e)
            } finally {
                isLoading = false
            }
        }
    }
}