package com.sifa.sifa_go.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import com.sifa.sifa_go.core.network.ServerConfig
import com.sifa.sifa_go.core.utils.LocationHelper
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.FiscalizadorHeartbeatRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.os.Build

class PresenceViewModel : ViewModel() {

    private var heartbeatJob: Job? = null

    private val _heartbeatTrigger = MutableStateFlow(0L)
    val heartbeatTrigger: StateFlow<Long> = _heartbeatTrigger.asStateFlow()

    @SuppressLint("HardwareIds")
    fun startHeartbeatEngine(context: Context, sessionManager: SessionManager) {
        if (heartbeatJob?.isActive == true) return

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "DESCONOCIDO"
        val brand = Build.BRAND ?: "Desconocida"
        val model = Build.MODEL ?: "Desconocido"
        val locationHelper = LocationHelper(context)

        heartbeatJob = viewModelScope.launch {
            sendHeartbeat(locationHelper, sessionManager, deviceId, brand, model)

            while (true) {
                delay(ServerConfig.HEARTBEAT_INTERVAL_MS)
                sendHeartbeat(locationHelper, sessionManager, deviceId, brand, model)
            }
        }
    }

    private suspend fun sendHeartbeat(
        locationHelper: LocationHelper,
        sessionManager: SessionManager,
        deviceId: String,
        brand: String,
        model: String
    ) {
        try {
            if (sessionManager.getToken() == null) return

            val location = locationHelper.getLocation()
            if (location == null) {
                println("SIFA GO - No se pudo obtener ubicación para el latido")
                return
            }

            val request = FiscalizadorHeartbeatRequest(
                latitud = location.latitude,
                longitud = location.longitude,
                deviceId = deviceId,
                marca = brand,
                modelo = model,
            )

            val response = CoreRetrofitClient.apiService.sendHeartbeat(request = request)
            if (response.isSuccessful) {
                _heartbeatTrigger.value = System.currentTimeMillis()
                println("Latido enviado exitosamente: Lat ${location.latitude}, Lng ${location.longitude}")
            } else {
                println("SIFA GO - Error enviando latido: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            println("SIFA GO - Error enviando latido: ${e.message}")
        }
    }

    fun stopHeartbeatEngine() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        println("Motor de latidos detenido.")
    }
}
