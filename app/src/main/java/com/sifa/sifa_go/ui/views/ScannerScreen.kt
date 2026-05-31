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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
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
import com.sifa.sifa_go.core.network.GpsStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.util.concurrent.Executor
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.MultiplePermissionsState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    cameraController: LifecycleCameraController,
    sifaViewModel: SifaViewModel = viewModel(), // Inyectamos el ViewModel
    coreViewModel: CoreViewModel = viewModel(),
    gpsStatus: GpsStatus = GpsStatus.Available,
    currentRoute: String = "scan",
    onPhotoConfirmed: (String) -> Unit
) {

    // Manejo de permisos para la cámara y ubicacion del dispositivo
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Manejo del contexto de la aplicación
    val context = LocalContext.current

    val activity = context as Activity

    // Detectar si android aún puede mostrar el popup
    val shouldShowRationale =
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )

    // lifecycle permite gestionar el ciclo de vida de la camara de forma automática
    // y de forma segura. Si se cierra la app o se destruye la vista, se destruye la camara.
    val lifecycle = LocalLifecycleOwner.current

    // Ejecutor para manejar la captura de la foto en el hilo principal
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // Instanciamos el State Holder que agrupa toda la UI
    val uiState = rememberScannerUiState()

    // Verificamos si hay algún proceso activo delegando a las vistas y al ViewModel
    val isShowingProcess = sifaViewModel.isLoading ||
            !sifaViewModel.detectedPlate.isNullOrEmpty() ||
            sifaViewModel.detectionError != null ||
            coreViewModel.vehicleData != null ||
            uiState.capturedPhotoPath != null ||
            sifaViewModel.isManualEntry

    // Interceptamos el botón físico "Atrás" del celular
    BackHandler(enabled = isShowingProcess) {
        // Luego, limpiamos la memoria
        sifaViewModel.clearProcess()
        coreViewModel.clearData()
        uiState.resetUi()
    }

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(uiState.capturedPhotoPath, permissionState.allPermissionsGranted) {
        if (uiState.capturedPhotoPath == null && permissionState.allPermissionsGranted) {
            sifaViewModel.startGpsCalibration()
        }
    }

    // INTERCAMBIO DE VISTAS
    when {
        coreViewModel.submitSuccess -> {
            // VISTA DEL EXITO AL EMITIR INFRACCION
            SuccessTicketView(
                sifaViewModel = sifaViewModel,
                coreViewModel = coreViewModel,
                onFinish = {
                    uiState.showTicketForm = false
                    uiState.capturedPhotoPath = null
                }
            )
        }

        sifaViewModel.isLoading -> {
            // VISTA DE CARGA
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        uiState.showTicketForm && coreViewModel.vehicleData != null -> {
            // VISTA DEL FORMULARIO DE INFRACCIÓN + CÁMARA OVERLAY
            TicketFormWithOverlay(
                sifaViewModel = sifaViewModel,
                coreViewModel = coreViewModel,
                cameraController = cameraController,
                lifecycle = lifecycle,
                mainExecutor = mainExecutor,
                evidencePhotoPaths = sifaViewModel.evidencePhotoPaths,
                isTakingEvidencePhoto = uiState.isTakingEvidencePhoto,
                onCancelClick = { uiState.showTicketForm = false },
                onAddPhotoClick = { uiState.isTakingEvidencePhoto = true },
                onOverlayClosed = { uiState.isTakingEvidencePhoto = false }
            )
        }

        coreViewModel.vehicleData != null -> {
            // VISTA DE INFORMACIÓN DEL VEHÍCULO
            VehicleInfoView(
                sifaViewModel = sifaViewModel,
                coreViewModel = coreViewModel,
                onIssueFineClick = { uiState.showTicketForm = true },
                onRestart = { uiState.capturedPhotoPath = null }
            )
        }

        sifaViewModel.isManualEntry || sifaViewModel.detectedPlate != null || sifaViewModel.detectionError != null -> {
            // VISTA DEL RESULTADO DE LA PATENTE
            PlateResultView(
                sifaViewModel = sifaViewModel,
                coreViewModel = coreViewModel,
                onRestart = {
                    uiState.capturedPhotoPath = null
                    sifaViewModel.isManualEntry = false // <-- APAGAMOS LA BANDERA AQUÍ
                }
            )
        }

        uiState.capturedPhotoPath != null -> {
            // VISTA PREVISUALIZACIÓN
            PreviewView(
                sifaViewModel = sifaViewModel,
                capturedPhotoPath = uiState.capturedPhotoPath!!,
                onRetake = { uiState.capturedPhotoPath = null },
            )
        }

        else -> {
            // VISTA BASE: CÁMARA EN VIVO
            LiveCameraView(
                sifaViewModel = sifaViewModel,
                cameraController = cameraController,
                lifecycle = lifecycle,
                mainExecutor = mainExecutor,
                permissionState = permissionState,
                gpsStatus = gpsStatus,
                isGPSCalibrating = sifaViewModel.isGPSCalibrating, // <-- Lee desde el ViewModel
                triggerGPSCalibration = { sifaViewModel.startGpsCalibration() }, // <-- Ejecuta el ViewModel
                onPhotoTaken = { path ->
                    uiState.capturedPhotoPath = path
                    sifaViewModel.evidencePhotoPaths.add(path)
                }
            )
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
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        cameraController.bindToLifecycle(lifecycle)
        onDispose {
            cameraController.unbind() // <-- ESTO EVITA QUE SE QUEDE PEGADA
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                implementationMode = PreviewView.ImplementationMode.COMPATIBLE

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

@Composable
private fun SuccessTicketView(
    sifaViewModel: SifaViewModel,
    coreViewModel: CoreViewModel,
    onFinish: () -> Unit
) {
    TicketSuccessScreen(
        onAnimationFinished = {
            sifaViewModel.clearProcess()
            coreViewModel.clearData()
            onFinish()
        }
    )
}

@Composable
private fun LoadingOverlay(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
        Text(message, modifier = Modifier.padding(top = 60.dp))
    }
}

@Composable
private fun TicketFormWithOverlay(
    sifaViewModel: SifaViewModel,
    coreViewModel: CoreViewModel,
    cameraController: LifecycleCameraController,
    lifecycle: LifecycleOwner,
    mainExecutor: Executor,
    evidencePhotoPaths: SnapshotStateList<String>,
    isTakingEvidencePhoto: Boolean,
    onCancelClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onOverlayClosed: () -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        TicketScreen(
            vehicleData = coreViewModel.vehicleData!!,
            tiposInfraccion = coreViewModel.tiposInfraccion,
            evidencePhotos = evidencePhotoPaths,
            latitude = sifaViewModel.latitude,
            longitude = sifaViewModel.longitude,
            isSubmitting = coreViewModel.isSubmittingInfraccion,
            onCancelClick = onCancelClick,
            onSubmitClick = { idInfraccion, observaciones, lat, lon ->
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                val fechaFiscalizacion = sifaViewModel.captureTime ?: java.time.LocalDateTime.now().format(formatter)
                val lugarFinal = sifaViewModel.currentAddress ?: "Ubicación GPS: $lat, $lon"

                val request = com.sifa.sifa_go.data.model.InfraccionCreateRequest(
                    lugar = lugarFinal,
                    fecha = fechaFiscalizacion,
                    latitud = lat?.toFloat() ?: 0f,
                    longitud = lon?.toFloat() ?: 0f,
                    patenteVehiculo = coreViewModel.vehicleData!!.patente,
                    idTipoInfraccion = idInfraccion,
                    observaciones = observaciones,
                    fechaCitacion = null
                )
                coreViewModel.submitInfraccion(request, evidencePhotoPaths.toList())
            },
            onAddPhotoClick = onAddPhotoClick,
            onRemovePhoto = { path ->
                sifaViewModel.removeEvidencePhoto(path)
            }
        )

        if (isTakingEvidencePhoto) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black)
            ) {
                Camera(cameraController = cameraController, lifecycle = lifecycle, modifier = Modifier.fillMaxSize())
                androidx.compose.material3.IconButton(
                    onClick = onOverlayClosed,
                    modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 16.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        takePicture(cameraController, context, mainExecutor) { path ->
                            evidencePhotoPaths.add(path)
                            onOverlayClosed()
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                    text = { Text("CAPTURAR EVIDENCIA") }
                )
            }
        }
    }
}

@Composable
private fun VehicleInfoView(
    sifaViewModel: SifaViewModel,
    coreViewModel: CoreViewModel,
    onIssueFineClick: () -> Unit,
    onRestart: () -> Unit
) {
    VehicleInfoScreen(
        vehicleData = coreViewModel.vehicleData!!,
        onIssueFineClick = onIssueFineClick,
        onNewScanClick = {
            sifaViewModel.clearProcess()
            coreViewModel.clearData()
            onRestart()
        }
    )
}

@Composable
private fun PlateResultView(
    sifaViewModel: SifaViewModel,
    coreViewModel: CoreViewModel,
    onRestart: () -> Unit
) {
    PlateResultScreen(
        initialPlate = sifaViewModel.detectedPlate,
        errorMessage = coreViewModel.errorMessage ?: sifaViewModel.detectionError,
        isLoading = coreViewModel.isLoading,
        isManualEntry = sifaViewModel.isManualEntry,
        onConsultClick = { finalPlate ->
            sifaViewModel.detectedPlate = finalPlate
            sifaViewModel.detectionError = null
            coreViewModel.fetchVehicleInfo(finalPlate)
        },
        onRetakePhoto = {
            sifaViewModel.clearProcess()
            coreViewModel.clearData()
            onRestart()
        }
    )
}

@Composable
private fun PreviewView(
    sifaViewModel: SifaViewModel,
    capturedPhotoPath: String,
    onRetake: () -> Unit
) {
    PreviewScreen(
        photoPath = capturedPhotoPath,
        onRetakePhoto = {
            sifaViewModel.removeEvidencePhoto(capturedPhotoPath)
            onRetake()
        },
        onSendPhoto = { finalPath ->
            sifaViewModel.uploadImageToBackend(finalPath)
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun LiveCameraView(
    sifaViewModel: SifaViewModel,
    cameraController: LifecycleCameraController,
    lifecycle: LifecycleOwner,
    mainExecutor: Executor,
    permissionState: MultiplePermissionsState,
    gpsStatus: GpsStatus,
    isGPSCalibrating: Boolean,
    triggerGPSCalibration: () -> Unit,
    onPhotoTaken: (String) -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                    sifaViewModel.captureTime = java.time.LocalDateTime.now().format(formatter)
                    takePicture(cameraController, context, mainExecutor) { path ->
                        onPhotoTaken(path)
                    }
                },
                containerColor = Color.White,
                contentColor = Color.Blue,
                icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara") },
                text = { Text("TOMAR FOTO") }
            )
        }
    ) { paddingValues ->
        if (permissionState.allPermissionsGranted) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                Camera(cameraController = cameraController, lifecycle = lifecycle, modifier = Modifier.fillMaxSize())
                ScannerOverlay()

                val isGpsAvailable = gpsStatus is GpsStatus.Available
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .clickable(enabled = isGpsAvailable && !isGPSCalibrating) { triggerGPSCalibration() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        isGPSCalibrating -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        !isGpsAvailable -> Icon(Icons.Default.LocationOff, contentDescription = "GPS desactivado", tint = Color.Red, modifier = Modifier.size(16.dp))
                        else -> sifaViewModel.gpsAccuracy?.let { accuracy ->
                            Icon(Icons.Default.MyLocation, contentDescription = "Recalibrar GPS", tint = if (accuracy < 10f) Color.Green else Color.Yellow, modifier = Modifier.size(16.dp))
                        }
                    }
                    val accuracyText = if (!isGpsAvailable) " GPS: --" else sifaViewModel.gpsAccuracy?.let { " GPS: ${it.toInt()}m" } ?: " GPS: --"
                    val accuracyColor = when {
                        !isGpsAvailable -> Color.Red
                        sifaViewModel.gpsAccuracy?.let { it < 10f } == true -> Color.Green
                        else -> Color.Yellow
                    }
                    Text(text = accuracyText, color = accuracyColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // El componente de permisos faltantes (puedes extraerlo a otra función si quieres)
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Icon(Icons.Default.LocationOn, contentDescription = "GPS", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Permisos requeridos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("La aplicación necesita acceso a la cámara y el GPS del dispositivo para escanear patentes.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                            Text("Conceder permiso")
                        }
                    }
                }
            }
        }
    }
}

// State Holder: para que no se pierda el estado de la UI
class ScannerUiState {
    var capturedPhotoPath by mutableStateOf<String?>(null)
    var showTicketForm by mutableStateOf(false)
    var isTakingEvidencePhoto by mutableStateOf(false)

    // Agrupamos la lógica de limpieza visual en un solo lugar
    fun resetUi() {
        capturedPhotoPath = null
        showTicketForm = false
        isTakingEvidencePhoto = false
    }
}

// Función Compose que recuerda el estado para que no se pierda al rotar o redibujar
@Composable
fun rememberScannerUiState(): ScannerUiState {
    return remember { ScannerUiState() }
}