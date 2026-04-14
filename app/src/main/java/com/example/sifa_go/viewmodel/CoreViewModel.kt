package com.example.sifa_go.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sifa_go.core.network.CoreRetrofitClient
import com.example.sifa_go.core.utils.SessionManager
import com.example.sifa_go.data.model.PlateInfoResponse
import kotlinx.coroutines.launch

class CoreViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    var vehicleData by mutableStateOf<PlateInfoResponse?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun fetchVehicleInfo(plate: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            vehicleData = null

            try {
                // 1. Obtenemos el token guardado en el celular
                val token = sessionManager.getToken() ?: ""

                // 2. Hacemos la petición añadiendo "Bearer " al inicio del token
                val response = CoreRetrofitClient.apiService.getPlateInfo(
                    token = "Bearer $token",
                    plate = plate
                )

                // 3. Guardamos los datos recibidos
                vehicleData = response

            } catch (e: Exception) {
                errorMessage = "Error al obtener datos del vehículo: ${e.message}"
                println("Core API Error: $e")
            } finally {
                isLoading = false
            }
        }
    }

    fun clearData() {
        vehicleData = null
        errorMessage = null
    }
}