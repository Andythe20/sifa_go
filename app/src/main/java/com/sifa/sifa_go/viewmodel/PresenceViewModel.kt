package com.sifa.sifa_go.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
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
import android.os.Build

class PresenceViewModel : ViewModel() {

    // variables para el envio de la actividad del fiscalizador
    private var heartbeatJob: Job? = null
    private val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L // 3 minutos

    // Válvula de tiempo para bloquear los rebotes de la calibración GPS
    private var lastSentTime = 0L

    private val _heartbeatTrigger = MutableStateFlow(0L)
    val heartbeatTrigger: StateFlow<Long> = _heartbeatTrigger.asStateFlow()

    /**
     * Inicia el motor de latidos. Recibe sus dependencias por parámetro.
     */
    @SuppressLint("HardwareIds")
    fun startHeartbeatEngine(context: Context, sessionManager: SessionManager) {
        if (heartbeatJob?.isActive == true) return

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "DESCONOCIDO"
        val brand = Build.BRAND ?: "Desconocida"
        val model = Build.MODEL ?: "Desconocido"
        val locationHelper = LocationHelper(context)

        heartbeatJob = viewModelScope.launch {
            while (true) {
                try {
                    if (sessionManager.getToken() != null) {
                        locationHelper.startPrecisionCalibration { location ->
                            val currentTime = System.currentTimeMillis()

                            if (currentTime - lastSentTime > 150000L) {

                                // se actualiza la valvula
                                lastSentTime = currentTime

                                val request = FiscalizadorHeartbeatRequest(
                                    latitud = location.latitude,
                                    longitud = location.longitude,
                                    deviceId = deviceId,
                                    marca = brand,
                                    modelo = model,
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
                                        println("SIFA GO - Error enviando latido: ${e.message}")
                                        // Si hay error de red, abrimos la válvula para que el intento 2/5 pueda probar suerte
                                        lastSentTime = 0L
                                    }
                                }
                            } else {
                                println("SIFA GO - Coordenada GPS de calibración descartada (Válvula de 3 min activa)")
                            }


                        }
                    }
                } catch (e: Exception) {
                    println("SIFA GO - Esperando permisos de GPS del usuario...")
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
        lastSentTime = 0L // Reseteamos la válvula al apagar
        println("Motor de latidos detenido.")
    }
}