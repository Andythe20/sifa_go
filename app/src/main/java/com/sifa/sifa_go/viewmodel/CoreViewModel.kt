package com.sifa.sifa_go.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File

class CoreViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    var vehicleData by mutableStateOf<PlateInfoResponse?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // variables para peticion a la api core de tipos de infracciones
    var tiposInfraccion by mutableStateOf<List<TipoInfraccionResponse>>(emptyList())
    var isLoadingTipos by mutableStateOf(false)

    // Estados para controlar el proceso de envío de multas
    var isSubmittingInfraccion by mutableStateOf(false) // Bloquea el botón en la UI
    var submitSuccess by mutableStateOf(false)          // Activa la navegación de salida al éxito

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

    // Función para obtener la lista de tipo de infracciones del backend
    fun fetchTiposInfraccion() {
        viewModelScope.launch {
            isLoadingTipos = true
            try {
                val token = sessionManager.getToken() ?: ""
                // Llamamos al nuevo endpoint
                tiposInfraccion = CoreRetrofitClient.apiService.getAllTipoInfracciones("Bearer $token")
            } catch (e: HttpException) {
                // Retrofit lanza HttpException cuando el backend responde con un error (400, 404, 500)
                errorMessage = when (e.code()) {
                    404 -> "Tipo de infraccion no encontrado"
                    401 -> "Sesión expirada o token inválido." // Esto lo atajaremos con biometría luego
                    503, 504 -> "El servicio nacional no está disponible en este momento."
                    else -> "Error del servidor (${e.code()}). Intente nuevamente."
                }
                println("Core API HTTP Error: ${e.code()} - ${e.message()}")

            } catch (e: Exception) {
                // Esto ocurre si no hay internet o el servidor está apagado (no hay respuesta HTTP)
                errorMessage = "Error de conexión. Compruebe su acceso a internet."
                println("Core API Error de Red: $e")

            } finally {
                isLoadingTipos = false
            }
        }
    }

    /**
     * Envía la infracción al servidor.
     * Si tiene éxito, activa [submitSuccess] para que la vista se cierre automáticamente.
     */
    fun submitInfraccion(
        request: InfraccionCreateRequest,
        imagePaths: List<String>
    ) {
        viewModelScope.launch {
            isSubmittingInfraccion = true
            errorMessage = null
            submitSuccess = false
            
            try {
                val token = sessionManager.getToken() ?: ""

                // Convertir el DTO a JSON RequestBody
                val jsonRequest = Gson().toJson(request)
                    .toRequestBody("application/json".toMediaTypeOrNull())

                // Convertir la lista de rutas en MultipartBody.Part
                val fotoParts = imagePaths.map { path ->
                    val file = File(path)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("fotos", file.name, requestFile)
                }

                // Enviar la petición
                val response = CoreRetrofitClient.apiService.createInfraccion(
                    token = "Bearer $token",
                    request = request,
                    fotos = fotoParts
                )

                // Imprimir para debuggear
                println(response)
                println(request)

                submitSuccess = true // Notifica a la UI que el proceso terminó bien
            } catch (e: HttpException) {
                errorMessage = "Error al guardar la infracción: ${e.code()}"
                println("Core API HTTP Error (submit): ${e.code()} - ${e.message()}")
            } catch (e: Exception) {
                errorMessage = "Error de conexión al guardar la infracción."
                println("Core API Error de Red (submit): $e")
            } finally {
                isSubmittingInfraccion = false
            }
        }
    }

    fun clearData() {
        vehicleData = null
        errorMessage = null
        submitSuccess = false
    }
}