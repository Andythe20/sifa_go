package com.example.sifa_go.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sifa_go.core.network.RetrofitClient
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SifaViewModel : ViewModel() {
    // Variable que guardará la ruta de la foto de forma global
    // Usamos mutableStateOf para que la interfaz se actualice si esto cambia
    var currentPhotoPath by mutableStateOf<String?>(null)

    // variables para manejar el estado de la API
    var isLoading by mutableStateOf(false)
    var rawJsonResponse by mutableStateOf<String?>(null)

    // Función para limpiar los datos cuando se termine una multa o se cancele
    fun clearProcess() {
        currentPhotoPath = null
        rawJsonResponse = null
    }

    // Función para enviar la imagen
    fun uploadImageToBackend(filePath: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val file = File(filePath)

                // Preparamos el archivo para enviarlo por HTTP
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // Hacemos la llamada a la API
                val response = RetrofitClient.apiService.detectPlate(body)

                // De la respuesta sacamos el primer elemento (si existe) y obtenemos la patente
                val patenteDetectada = response.result.firstOrNull()?.plate ?: "No detectada"

                println("¡La patente detectada es: $patenteDetectada!")

                // Convertimos la respuesta a JSON formateado para mostrarlo en pantalla
                val gson = GsonBuilder().setPrettyPrinting().create()
                rawJsonResponse = gson.toJson(response)

            } catch (e: Exception) {
                // Si falla (por ej. si el PC está apagado), mostramos el error
                rawJsonResponse = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}