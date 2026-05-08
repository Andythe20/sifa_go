package com.sifa.sifa_go.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.PlateInfoResponse
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
                // Obtenemos el token guardado en el celular
                val token = sessionManager.getToken() ?: ""

                // Hacemos la petición añadiendo "Bearer " al inicio del token
                val response = CoreRetrofitClient.apiService.getPlateInfo(
                    token = "Bearer $token",
                    id = plate
                )

                // Guardamos los datos recibidos
                vehicleData = response

            } catch (e: HttpException) {
                // Retrofit lanza HttpException cuando el backend responde con un error (400, 404, 500)
                errorMessage = when (e.code()) {
                    404 -> "Vehículo no encontrado. Verifique que la patente ingresada sea correcta."
                    401 -> "Sesión expirada o token inválido." // Esto lo atajaremos con biometría luego
                    503, 504 -> "El servicio nacional no está disponible en este momento."
                    else -> "Error del servidor (${e.code()}). Intente nuevamente."
                }
                println("Core API HTTP Error: ${e.code()} - ${e.message()}")

            } catch (e: Exception) {
                // Esto ocurre si no hay internet o el servidor está apagado (no hay respuesta HTTP)
                errorMessage = "Error de conexión. Compruebe su acceso a internet."
                println("Core API Error de Red: $e")

            }finally {
                isLoading = false
            }
        }
    }

    fun clearData() {
        vehicleData = null
        errorMessage = null
    }
}