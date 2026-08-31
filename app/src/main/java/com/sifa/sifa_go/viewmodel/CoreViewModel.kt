package com.sifa.sifa_go.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.local.db.SifaDatabase
import com.sifa.sifa_go.data.local.entity.SyncStatus
import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import com.sifa.sifa_go.infrastructure.offline.OfflineInfraccionSender
import com.sifa.sifa_go.infrastructure.offline.OfflineQueueRepositoryImpl
import com.sifa.sifa_go.infrastructure.offline.SyncResult
import com.sifa.sifa_go.domain.model.PendingInfraccion
import com.sifa.sifa_go.domain.repository.OfflineQueueRepository
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class CoreViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    /** Cola offline de infracciones (persistida en SQLite vía Room). */
    private val offlineQueueRepository: OfflineQueueRepository by lazy {
        OfflineQueueRepositoryImpl(SifaDatabase.getInstance(application).pendingInfraccionDao())
    }

    var vehicleData by mutableStateOf<PlateInfoResponse?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // variables para peticion a la api core de tipos de infracciones
    var tiposInfraccion by mutableStateOf<List<TipoInfraccionResponse>>(emptyList())
    var isLoadingTipos by mutableStateOf(false)

    // Estados para controlar el proceso de envío de multas
    var isSubmittingInfraccion by mutableStateOf(false) // Bloquea el botón en la UI
    var submitSuccess by mutableStateOf(false)          // Activa la navegación de salida al éxito

    /** true si la infracción se guardó en la cola local (sin enviar) por falta de conexión. */
    var submittedOffline by mutableStateOf(false)

    /** Cantidad de infracciones pendientes de enviar en la cola offline. */
    var pendingCount by mutableIntStateOf(0)
        private set

    /** Infracciones aún no enviadas al backend (cola offline), para reflejarlas en la UI. */
    var pendingInfracciones by mutableStateOf<List<PendingInfraccion>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            offlineQueueRepository.observeSyncableQueue()
                .collect { queued ->
                    val onlyPending = queued.filter { it.status == SyncStatus.PENDING }
                    pendingInfracciones = onlyPending
                    pendingCount = onlyPending.size
                }
        }
    }


    fun fetchVehicleInfo(plate: String) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            vehicleData = null

            try {
                val response = CoreRetrofitClient.apiService.getPlateInfo(
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
                val paginatedResponse = CoreRetrofitClient.apiService.getAllTipoInfracciones()

                if (paginatedResponse.isSuccessful){
                    // Extraemos la lista plana desde la llave 'content'
                    val pageResponse = paginatedResponse.body()

                    // Si por alguna razón la respuesta completa o el content vienen nulos,
                    // usamos el operador elvis (?:) para asignar una lista vacía segura.
                    tiposInfraccion = pageResponse?.content ?: emptyList()
                } else {
                    errorMessage = "Error del servidor: ${paginatedResponse.code()}"
                }

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
     *
     * Modo offline: la infracción siempre se persiste primero en la cola local.
     * - Si hay conexión, se envía al momento.
     * - Si no hay conexión, queda pendiente y se envía automáticamente al reconectar
     *   (ver OfflineSyncCoordinator / OfflineSyncWorker). En ese caso activa [submittedOffline].
     */
    fun submitInfraccion(
        request: InfraccionCreateRequest,
        imagePaths: List<String>
    ) {
        viewModelScope.launch {
            isSubmittingInfraccion = true
            errorMessage = null
            submitSuccess = false
            submittedOffline = false

            // 1. Calcular la fecha de citación (margen mínimo legal de 2 semanas, próximo jueves a las 09:00)
            val requestConCitacion = request.copy(fechaCitacion = calcularFechaCitacion(request.fecha))

            // 2. Copiar las fotos de evidencia a un directorio propio de la cola (snapshot inmutable).
            //    Así, cuando el flujo de escaneo borre sus archivos originales (clearProcess), la cola
            //    conserva copias para poder reenviar en segundo plano sin fallar.
            val queueImagePaths = copyToOfflineQueue(imagePaths)

            // 3. Persistir SIEMPRE en la cola local para no perder la infracción
            val pendingId = offlineQueueRepository.enqueue(requestConCitacion, queueImagePaths)

            try {
                // 4. Enviar al backend si hay conexión (el sender captura todos los casos)
                val result = OfflineInfraccionSender.send(requestConCitacion, queueImagePaths)
                when (result) {
                    SyncResult.Success -> {
                        // Envío inmediato exitoso: ya no es necesario conservar el registro ni sus copias.
                        queueImagePaths.forEach { com.sifa.sifa_go.core.utils.ImageUtils.deleteImageFile(it) }
                        offlineQueueRepository.deleteById(pendingId)
                        println("Infracción enviada exitosamente: $requestConCitacion")
                        submitSuccess = true
                    }
                    // Sin conexión o error transitorio: queda pendiente para envío automático
                    is SyncResult.TransientNetworkError -> {
                        android.util.Log.w("OfflineVM", "Transitorio: ${result.failureReason}")
                        errorMessage = "Sin conexión a internet. La infracción se guardó y se enviará automáticamente al reconectar."
                        submittedOffline = true
                        submitSuccess = true
                    }
                    is SyncResult.PermanentError -> {
                        offlineQueueRepository.markFailed(pendingId, result.failureReason)
                        android.util.Log.e("OfflineVM", "Falló: ${result.failureReason}")
                        errorMessage = "No se pudo guardar la infracción: ${result.failureReason}"
                    }
                }
            } catch (e: Exception) {
                // Red de seguridad por si algo lanza fuera del sender (solo log; no marcamos FAILED
                // porque probablemente sea transitorio - el worker/coordinador reintentará).
                android.util.Log.e("OfflineVM", "Excepción inesperada en submit: ${e.javaClass.simpleName} ${e.message}", e)
            } finally {
                isSubmittingInfraccion = false
            }
        }
    }

    /** Calcula la fecha de citación reglamentaria (nextOrSame jueves + 2 semanas, 09:00). */
    private fun calcularFechaCitacion(fechaFiscalizacion: String): String {
        val fechaInfraccionDateTime = LocalDateTime.parse(fechaFiscalizacion)
        val fechaMinimaMargen = fechaInfraccionDateTime.plusDays(14)
        val juevesCitacion = fechaMinimaMargen.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))
        val fechaCitacionFinal = juevesCitacion
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        val formatterISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        return fechaCitacionFinal.format(formatterISO)
    }

    /**
     * Copia las fotos de evidencia a un directorio propiedad de la cola offline,
     * de modo que sean inmunes al borrado que hace el flujo de escaneo (clearProcess).
     * @return las rutas de las copias creadas (se omiten las que no existan).
     */
    private fun copyToOfflineQueue(imagePaths: List<String>): List<String> {
        val queueDir = File(getApplication<Application>().filesDir, "offline_queue").apply { mkdirs() }
        return imagePaths.mapNotNull { srcPath ->
            val src = File(srcPath)
            if (!src.exists()) return@mapNotNull null
            val dest = File(queueDir, "q_${System.currentTimeMillis()}_${src.name}")
            runCatching { src.copyTo(dest, overwrite = true).absolutePath }
                .onFailure { android.util.Log.e("OfflineVM", "No se pudo copiar foto a la cola: $srcPath", it) }
                .getOrNull()
        }
    }


    fun clearData() {
        vehicleData = null
        errorMessage = null
        submitSuccess = false
        submittedOffline = false
    }
}