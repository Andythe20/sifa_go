package com.sifa.sifa_go.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import com.sifa.sifa_go.core.utils.LocationHelper
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.FiscalizadorHeartbeatRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PresenceViewModel : ViewModel() {

    // variables para el envio de la actividad del fiscalizador
    private var heartbeatJob: Job? = null
    private val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L // 3 minutos

    private val _heartbeatTrigger = MutableStateFlow(0L)
    val heartbeatTrigger: StateFlow<Long> = _heartbeatTrigger.asStateFlow()

    /**
     * Inicia el motor de latidos. Recibe sus dependencias por parámetro.
     */
    fun startHeartbeatEngine(context: Context, sessionManager: SessionManager) {
        if (heartbeatJob?.isActive == true) return

        val locationHelper = LocationHelper(context)

        heartbeatJob = viewModelScope.launch {
            while (true) {
                if (sessionManager.getToken() != null) {
                    locationHelper.startPrecisionCalibration { location ->
                        val request = FiscalizadorHeartbeatRequest(
                            latitud = location.latitude,
                            longitud = location.longitude
                        )

                        viewModelScope.launch {
                            try {
                                val response = CoreRetrofitClient.apiService.sendHeartbeat(
                                    request = request
                                )
                                if (response.isSuccessful) {
                                    _heartbeatTrigger.value = System.currentTimeMillis()
                                    println("Latido enviado exitosamente: Lat ${location.latitude}, Lng ${location.longitude}")
                                }
                            } catch (e: Exception) {
                                println("Error enviando latido: ${e.message}")
                            }
                        }
                    }
                }

                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    /**
     * Detiene el motor de latidos de forma segura.
     */
    fun stopHeartbeatEngine() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        println("Motor de latidos detenido.")
    }
}