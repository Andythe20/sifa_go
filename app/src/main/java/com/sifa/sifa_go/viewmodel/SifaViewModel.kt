package com.sifa.sifa_go.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sifa.sifa_go.core.image.toCleanMultipartPart
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import com.sifa.sifa_go.core.network.RetrofitClient
import com.sifa.sifa_go.core.utils.SessionManager
import com.sifa.sifa_go.data.model.InfraccionHistoryItem
import com.sifa.sifa_go.exception.NetworkErrorHandler
import kotlinx.coroutines.launch
import java.io.File

class SifaViewModel(application: Application) : AndroidViewModel(application) {

    // Instanciamos el manejador de sesión
    private val sessionManager = SessionManager(application)

    // "guarda" el trabajo de la corrutina de la calibracion del gps
    private var gpsTimerJob: kotlinx.coroutines.Job? = null

    // para saber si la patente se ingresará manualmente
    var isManualEntry by mutableStateOf(false)

    // Variable que guardará la ruta de la foto de forma global
    // Usamos mutableStateOf para que la interfaz se actualice si esto cambia
    var currentPhotoPath by mutableStateOf<String?>(null)

    // para guardar fotos de evidencia
    val evidencePhotoPaths = androidx.compose.runtime.mutableStateListOf<String>()

    // variables para manejar el estado de la API
    var isLoading by mutableStateOf(false)
    var detectedPlate by mutableStateOf<String?>(null)
    var detectionError by mutableStateOf<String?>(null)

    // Coordenadas GPS
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var currentAddress by mutableStateOf<String?>(null)

    // Hora exacta de la fiscalización (capturada al tomar la foto)
    var captureTime by mutableStateOf<String?>(null)

    // Instancia del manejador de GPS inyectada en el ViewModel
    private val locationHelper = com.sifa.sifa_go.core.utils.LocationHelper(application)

    // Estado que le dirá a la UI cuándo mostrar el ícono de "Cargando GPS"
    var isGPSCalibrating by mutableStateOf(false)

    // --- NUEVAS VARIABLES PARA CALIBRACIÓN ---
    // Guardamos la precisión para saber cuál es el mejor de los 5 intentos
    var gpsAccuracy by mutableStateOf<Float?>(null)

    // Contador para los logs de calibración
    var gpsAttemptCount by mutableIntStateOf(0)

    // Nombre del usuario logueado
    var currentUsername by mutableStateOf(sessionManager.getUsername() ?: "")

    fun getSessionIat(): Long = sessionManager.getTokenIat()

    // Variables para el historial de infracciones (incluye paginacion)
    var infractionsHistory by mutableStateOf<List<InfraccionHistoryItem>>(emptyList())
    var historyLoading by mutableStateOf(false)
    var historyError by mutableStateOf<String?>(null)
    var currentPage by mutableIntStateOf(0)
    var totalPages by mutableIntStateOf(0)
    var totalElements by mutableIntStateOf(0)
    var isFirstPage by mutableStateOf(true)
    var isLastPage by mutableStateOf(true)

    fun removeEvidencePhoto(path: String) {
        com.sifa.sifa_go.core.utils.ImageUtils.deleteImageFile(path)
        evidencePhotoPaths.remove(path)
    }

    /**
     * Orquesta el encendido del GPS, la recolección de las 5 muestras y la geocodificación.
     */
    fun startGpsCalibration() {
        if (isGPSCalibrating) return // Evita múltiples llamadas simultáneas si el usuario hace mucho clic

        // Cancelamos cualquier temporizador "fantasma" que haya quedado vivo
        gpsTimerJob?.cancel()

        isGPSCalibrating = true
        gpsAttemptCount = 0 // Reiniciamos el contador de la ráfaga
        gpsAccuracy = null  // Reiniciamos la mejor precisión

        Log.d("GPS_SIFA", "Iniciando ráfaga de 5 calibraciones desde ViewModel...")

        locationHelper.startPrecisionCalibration { location ->
            // Procesamos la coordenada usando tu lógica actual
            processCalibrationStep(location)

            // Pedimos la dirección legible en texto
            locationHelper.getAddressFromLocation(location.latitude, location.longitude) { address ->
                if (address != null) {
                    currentAddress = address
                    Log.d("GPS_SIFA", "Dirección obtenida: $address")
                }
            }
        }

        // Guardamos la nueva tarea en la variable
        gpsTimerJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            isGPSCalibrating = false
        }
    }

    /**
     * Procesa cada uno de los 5 intentos de ubicación.
     * Se queda con la que tenga mejor precisión (accuracy menor).
     */
    fun processCalibrationStep(location: android.location.Location) {
        gpsAttemptCount++
        val currentAccuracy = location.accuracy

        // Log para monitorear la ráfaga de 5 intentos en Logcat
        Log.d("GPS_SIFA", "Calibrando: Intento $gpsAttemptCount/5 | Precisión: ${currentAccuracy}m")

        // Si es el primer intento o si este nuevo intento es más preciso que el anterior, guardamos
        if (gpsAccuracy == null || currentAccuracy < (gpsAccuracy ?: Float.MAX_VALUE)) {
            latitude = location.latitude
            longitude = location.longitude
            gpsAccuracy = currentAccuracy
            Log.d(
                "GPS_SIFA",
                "Nueva mejor ubicación capturada: ${location.latitude}, ${location.longitude}"
            )
        }
    }

    // Función para limpiar los datos cuando se termine una multa o se cancele
    fun refreshCurrentLocation() {
        viewModelScope.launch {
            val location = locationHelper.getLocation()
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
                gpsAccuracy = location.accuracy
                locationHelper.getAddressFromLocation(location.latitude, location.longitude) { address ->
                    currentAddress = address
                }
            }
        }
    }

    fun clearProcess() {
        // Borramos los archivos físicos primero
        evidencePhotoPaths.forEach { path ->
            com.sifa.sifa_go.core.utils.ImageUtils.deleteImageFile(path)
        }
        // Vaciamos la lista
        evidencePhotoPaths.clear()
        currentPhotoPath = null
        detectedPlate = null
        detectionError = null
        latitude = null
        longitude = null
        currentAddress = null
        captureTime = null
        // Limpiamos también la calibración
        gpsAccuracy = null
        gpsAttemptCount = 0
        isGPSCalibrating = false
        gpsTimerJob?.cancel()
        isManualEntry = false
    }

    // Función para enviar la imagen
    fun uploadImageToBackend(filePath: String) {
        viewModelScope.launch {
            isLoading = true
            // Limpiamos estados anteriores
            detectedPlate = null
            detectionError = null
            try {
                val file = File(filePath)
                val body = file.toCleanMultipartPart("file")

                val response = RetrofitClient.apiService.detectPlate(
                    file = body
                )

                // Verificamos si la IA encontró una patente en la foto
                val plateResult = response.result.firstOrNull()

                if (plateResult != null && plateResult.success && !plateResult.plate.isNullOrEmpty()) {
                    // IA exitosa
                    detectedPlate = plateResult.plate.trim()
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

    /**
     * Carga el historial de infracciones paginado desde el backend.
     * Actualiza [infractionsHistory], [currentPage], [totalPages], [totalElements],
     * [isFirstPage] e [isLastPage] segun la respuesta.
     *
     * @param date filtro por fecha (YYYY-MM-DD)
     * @param page numero de pagina a solicitar (0-indexado, default 0)
     */
    fun loadInfractionsHistory(date: String, page: Int = 0) {
        viewModelScope.launch {

            historyLoading = true
            historyError = null

            try {
                val user = sessionManager.getUsername() ?: ""
                val response = CoreRetrofitClient.apiService.getInfractionsHistory(
                    startDate = date,
                    endDate = date,
                    user = user,
                    page = page,
                    size = 10
                )

                if (response.isSuccessful) {
                    val pageResponse = response.body()

                    infractionsHistory = (pageResponse?.content ?: emptyList())
                        .filter { it.id != null }
                        .sortedByDescending { it.id!!.toIntOrNull() ?: 0 }
                    currentPage = pageResponse?.pageNumber ?: 0
                    totalPages = pageResponse?.totalPages ?: 0
                    totalElements = pageResponse?.totalElements ?: 0
                    isFirstPage = pageResponse?.isFirst ?: true
                    isLastPage = pageResponse?.isLast ?: true
                } else {
                    historyError = "Error del servidor: ${response.code()}"
                }
            } catch (e: Exception) {
                historyError = "Error al cargar el historial: ${NetworkErrorHandler.getExceptionMessage(e)}"
                println(e)
            } finally {
                historyLoading = false
            }
        }
    }
}