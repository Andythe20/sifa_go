package com.sifa.sifa_go.ui.views

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sifa.sifa_go.viewmodel.CoreViewModel
import com.sifa.sifa_go.viewmodel.SifaViewModel
import com.sifa.sifa_go.core.utils.ImageUtils
import com.sifa.sifa_go.core.utils.LocationHelper
import com.sifa.sifa_go.core.utils.SessionManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.util.concurrent.Executor
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.LocationOn
import androidx.core.app.ActivityCompat


// Vista de camara y permisos
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    cameraController: LifecycleCameraController,
    sifaViewModel: SifaViewModel = viewModel(), // Inyectamos el ViewModel
    coreViewModel: CoreViewModel = viewModel(),
    onPhotoConfirmed: (String) -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val context = LocalContext.current
    val activity = context as Activity

    // Detectar si android aún puede mostrar el popup
    val shouldShowRationale =
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )

    val lifecycle = LocalLifecycleOwner.current
    val locationHelper = remember { LocationHelper(context) }

    // Ejecutor para manejar la captura de la foto en el hilo principal
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // ¿Tenemos una foto temporal para revisar?
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }

    // Variable para controlar si mostramos el formulario de multa
    var showTicketForm by remember { mutableStateOf(false) }

    // variable para adjuntar todas las fotos que se suban al backend
    val evidencePhotoPaths = remember { mutableStateListOf<String>() }

    // para saber si estamos sacando una foto extra
    var isTakingEvidencePhoto by remember { mutableStateOf(false) }

    // Verificamos si hay algún proceso activo en pantalla que no sea la cámara en vivo
    val isShowingProcess = sifaViewModel.isLoading ||
            sifaViewModel.detectedPlate != null ||
            sifaViewModel.detectionError != null ||
            coreViewModel.vehicleData != null ||
            capturedPhotoPath != null

    // Interceptamos el botón físico "Atrás" del celular
    BackHandler(enabled = isShowingProcess) {
        // Borramos TODAS las fotos de la sesión actual
        evidencePhotoPaths.forEach { path ->
            ImageUtils.deleteImageFile(path)
        }
        evidencePhotoPaths.clear() // Vaciamos la lista

        // Luego, limpiamos la memoria
        sifaViewModel.clearProcess()
        coreViewModel.clearData()
        capturedPhotoPath = null
    }

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    // Cada vez que entramos a la cámara o reiniciamos el proceso, intentamos capturar GPS
    // MEJORA: Ahora realiza una ráfaga de 5 calibraciones para mayor exactitud
    LaunchedEffect(capturedPhotoPath, permissionState.allPermissionsGranted) {
        if (capturedPhotoPath == null && permissionState.allPermissionsGranted) {
            Log.d("GPS_SIFA", "Iniciando ráfaga de 5 calibraciones de precisión...")
            locationHelper.startPrecisionCalibration { location ->
                // Enviamos cada intento al ViewModel para que capture el más exacto
                sifaViewModel.processCalibrationStep(location)

                // MEJORA: Obtener dirección legible una vez tengamos coordenadas (Geocoding)
                locationHelper.getAddressFromLocation(
                    location.latitude,
                    location.longitude
                ) { address ->
                    if (address != null) {
                        mainExecutor.execute {
                            Log.d("GPS_SIFA", "Dirección obtenida: $address")
                        }
                        sifaViewModel.currentAddress = address
                    }
                }
            }
        }
    }

    // INTERCAMBIO DE VISTAS
    if (coreViewModel.submitSuccess) {
        // VISTA DE EXITO AL REGISTRAR INFRACCION
        TicketSuccessScreen(
            onAnimationFinished = {
                // Esto se ejecuta cuando se termina la animación

                // borramos todas las evidencias fisicas
                evidencePhotoPaths.forEach { path ->
                    ImageUtils.deleteImageFile(path)
                }
                evidencePhotoPaths.clear()

                // Cerramos el formulario y limpiamos la ruta de la foto de la UI
                showTicketForm = false
                capturedPhotoPath = null

                // limpiamos memoria de los viewmodels
                sifaViewModel.clearProcess()
                coreViewModel.clearData()
            }
        )
    } else if (sifaViewModel.isLoading) {
        // VISTA DE CARGA
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Procesando imagen con IA...", modifier = Modifier.padding(top = 60.dp))
        }
    } else if (showTicketForm && coreViewModel.vehicleData != null) {
        // VISTA DEL FORMULARIO DE INFRACCIÓN

        val context = LocalContext.current
        val sessionManager = remember { SessionManager(context) }
        val authToken = sessionManager.getToken() ?: ""

        Box(modifier = Modifier.fillMaxSize()) {
            TicketScreen(
                vehicleData = coreViewModel.vehicleData!!,
                tiposInfraccion = coreViewModel.tiposInfraccion,
                authToken = authToken,
                evidencePhotos = evidencePhotoPaths,
                latitude = sifaViewModel.latitude,
                longitude = sifaViewModel.longitude,
                isSubmitting = coreViewModel.isSubmittingInfraccion,
                onCancelClick = { showTicketForm = false }, // Vuelve a la ficha del vehículo
                onSubmitClick = { idInfraccion, observaciones, lat, lon ->
                    // Usamos la fecha capturada al momento de la foto, o la actual como fallback
                    val formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                    val fechaFiscalizacion =
                        sifaViewModel.captureTime ?: java.time.LocalDateTime.now().format(formatter)

                    // Usamos la dirección obtenida por Geocoding, o las coordenadas como fallback
                    val lugarFinal = sifaViewModel.currentAddress ?: "Ubicación GPS: $lat, $lon"

                    // Construimos el objeto que espera el Backend
                    val request = com.sifa.sifa_go.data.model.InfraccionCreateRequest(
                        lugar = lugarFinal,
                        fecha = fechaFiscalizacion,
                        latitud = lat?.toFloat() ?: 0f,
                        longitud = lon?.toFloat() ?: 0f,
                        patenteVehiculo = coreViewModel.vehicleData!!.patente,
                        idTipoInfraccion = idInfraccion,
                        observaciones = observaciones,
                    )

                    // Disparamos la petición POST
                    coreViewModel.submitInfraccion(request, evidencePhotoPaths)
                },
                onAddPhotoClick = {
                    isTakingEvidencePhoto = true // Activamos la cámara overlay
                }
            )

            // La Cámara Overlay (Solo se dibuja si isTakingEvidencePhoto es true)
            if (isTakingEvidencePhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black) // Fondo negro para tapar el formulario
                ) {
                    // Reutilizamos tu componente de cámara
                    Camera(
                        cameraController = cameraController,
                        lifecycle = lifecycle,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Botón para cerrar la cámara y volver al formulario
                    androidx.compose.material3.IconButton(
                        onClick = { isTakingEvidencePhoto = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 40.dp, start = 16.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }

                    // Botón para capturar la nueva foto
                    ExtendedFloatingActionButton(
                        onClick = {
                            takePicture(cameraController, context, mainExecutor) { path ->
                                // Agregamos la nueva ruta a la lista
                                evidencePhotoPaths.add(path)
                                // Cerramos el overlay
                                isTakingEvidencePhoto = false
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                        text = { Text("CAPTURAR EVIDENCIA") }
                    )
                }
            }
        }

    } else if (coreViewModel.vehicleData != null) {
        // VISTA DE INFORMACIÓN DEL VEHÍCULO
        VehicleInfoScreen(
            vehicleData = coreViewModel.vehicleData!!,
            onIssueFineClick = {
                showTicketForm = true // se muestra el formulario de la infraccion
            },
            onNewScanClick = {
                // limpiamos la foto física antes de reiniciar el proceso
                evidencePhotoPaths.forEach { path ->
                    ImageUtils.deleteImageFile(path)
                }
                evidencePhotoPaths.clear()

                // Limpiamos AMBOS ViewModel para reiniciar todo desde cero
                capturedPhotoPath = null
                sifaViewModel.clearProcess()
                coreViewModel.clearData()
            }
        )
    } else if (sifaViewModel.detectedPlate != null || sifaViewModel.detectionError != null) {
        // VISTA DEL RESULTADO DE LA PATENTE
        PlateResultScreen(
            initialPlate = sifaViewModel.detectedPlate,
            errorMessage = coreViewModel.errorMessage
                ?: sifaViewModel.detectionError, // Mostramos error de IA o del Core
            isLoading = coreViewModel.isLoading, // Le pasamos el estado de carga del Core API
            onConsultClick = { finalPlate ->
                // Borramos errores anteriores por el plate detector service en caso de que core service devuelva uno.
                sifaViewModel.detectedPlate = finalPlate
                sifaViewModel.detectionError = null

                // Disparamos la consulta al Core
                coreViewModel.fetchVehicleInfo(finalPlate)
            },
            onRetakePhoto = {
                // limpiamos la foto física antes de reiniciar el proceso
                evidencePhotoPaths.forEach { path ->
                    ImageUtils.deleteImageFile(path)
                }
                evidencePhotoPaths.clear()

                capturedPhotoPath = null
                sifaViewModel.clearProcess()
                coreViewModel.clearData()
            }
        )

        // Si el CoreViewModel está cargando, mostramos un feedback
        if (coreViewModel.isLoading) {
            Text("Consultando base de datos nacional...")
        }
    } else if (capturedPhotoPath != null) {

        // VISTA PREVISUALIZACIÓN
        PreviewScreen(
            photoPath = capturedPhotoPath!!,
            onRetakePhoto = {
                // limpiamos la foto física antes de reiniciar el proceso
                ImageUtils.deleteImageFile(capturedPhotoPath)
                evidencePhotoPaths.remove(capturedPhotoPath)

                // Al volver a null, Jetpack Compose vuelve a dibujar la cámara instantáneamente
                capturedPhotoPath = null
            },
            onSendPhoto = { finalPath ->
                // se dispara peticion al backend plate detector
                sifaViewModel.uploadImageToBackend(finalPath)
            }
        )

    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButtonPosition = FabPosition.Center,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        val formatter =
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                        sifaViewModel.captureTime = java.time.LocalDateTime.now().format(formatter)
                        Log.d(
                            "SIFA_TIME",
                            "Hora de fiscalización capturada (Truco UTC): ${sifaViewModel.captureTime}"
                        )

                        takePicture(
                            cameraController,
                            context,
                            mainExecutor,
                        ) { path ->
                            capturedPhotoPath = path
                            evidencePhotoPaths.add(path)
                        }
                    },
                    containerColor = Color.White, // Puedes poner el color principal de tu app
                    contentColor = Color.Blue,
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara") },
                    text = { Text("TOMAR FOTO") }
                )
            }
        ) { paddingValues ->
            if (permissionState.allPermissionsGranted) {
                // Usamos un Box para poder apilar el overlay sobre la cámara
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 1. La vista de la cámara al fondo
                    Camera(
                        cameraController = cameraController,
                        lifecycle = lifecycle,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. El recuadro con el texto superpuesto
                    ScannerOverlay()

                    // Indicador visual del estado del GPS (Opcional)
                    sifaViewModel.gpsAccuracy?.let { accuracy ->
                        Text(
                            text = "GPS: ${accuracy.toInt()}m",
                            color = if (accuracy < 10f) Color.Green else Color.Yellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        )
                    }
                }
            } else {
                // Si no tenemos permisos, mostramos un feedback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cámara",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "GPS",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Permisos requeridos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "La aplicación necesita acceso a la cámara y el GPS del dispositivo para escanear patentes.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    permissionState.launchMultiplePermissionRequest()
                                }
                            ) {
                                Text("Conceder permiso")
                            }
                        }
                    }
                }
            }
        }
    }

}

// camara
@Composable
fun Camera(
    cameraController: LifecycleCameraController,
    lifecycle: LifecycleOwner,
    modifier: Modifier = Modifier,
) {

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Solo le pasamos el controlador
                controller = cameraController

                // Vinculamos al ciclo de vida (Sin onRelease ni DisposableEffect)
                cameraController.bindToLifecycle(lifecycle)
            }
        }
    )
}

// funcion para sacar fotos
private fun takePicture(
    cameraController: LifecycleCameraController,
    context: Context,
    executor: Executor,
    onPhotoTaken: (String) -> Unit
) {
    // Creamos un archivo temporal en la caché de la app
    val photoFile = File(context.cacheDir, "sifa_photo_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    cameraController.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Movemos la foto al almacenamiento persistente
                val permanentPath = savePhotoToPersistentStorage(context, photoFile)

                // eliminamos la foto en caché, ya no la necesitamos
                if (photoFile.exists()) {
                    photoFile.delete()
                }

                // Devolvemos la ruta final y segura
                onPhotoTaken(permanentPath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Error al tomar la foto", exception)
            }
        }
    )
}

// recuadro con texto para alinear patente
@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val rectWidth = canvasWidth * 0.85f // Ocupa el 75% del ancho
            val rectHeight = rectWidth * 0.4f   // Proporción rectangular

            val left = (canvasWidth - rectWidth) / 2
            val top = (canvasHeight - rectHeight) / 2
            val right = left + rectWidth
            val bottom = top + rectHeight

            // Dibujamos solo el fondo del recuadro con un tono blanco semi-transparente.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f), // Ajusta el 0.2f para más o menos blanco
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(12.dp.toPx())
            )

            // Dibujar solo las esquinas usando un Path
            val cornerLength = 80f // Qué tan largas quieres que sean las líneas de las esquinas
            val cornerPath = Path().apply {
                // Esquina Superior Izquierda
                moveTo(left, top + cornerLength)
                lineTo(left, top)
                lineTo(left + cornerLength, top)

                // Esquina Superior Derecha
                moveTo(right - cornerLength, top)
                lineTo(right, top)
                lineTo(right, top + cornerLength)

                // Esquina Inferior Derecha
                moveTo(right, bottom - cornerLength)
                lineTo(right, bottom)
                lineTo(right - cornerLength, bottom)

                // Esquina Inferior Izquierda
                moveTo(left + cornerLength, bottom)
                lineTo(left, bottom)
                lineTo(left, bottom - cornerLength)
            }

            // Trazar el Path que acabamos de crear
            drawPath(
                path = cornerPath,
                color = Color.White,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round // Suaviza las puntas de las líneas
                )
            )

        }

        // El texto sobre el recuadro
        Text(
            text = "ALINEE LA PATENTE CON EL RECUADRO",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 170.dp)

        )
    }
}

// guardar foto tomada en carpeta privada.
private fun savePhotoToPersistentStorage(context: Context, tempFile: File): String {
    return ImageUtils.compressImage(context, tempFile)
}