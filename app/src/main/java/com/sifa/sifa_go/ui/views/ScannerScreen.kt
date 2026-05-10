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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File
import java.util.concurrent.Executor


// Vista de camara y permisos
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    cameraController: LifecycleCameraController,
    sifaViewModel: SifaViewModel = viewModel(), // Inyectamos el ViewModel
    coreViewModel: CoreViewModel = viewModel(),
    onPhotoConfirmed: (String) -> Unit
){
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val locationHelper = remember { LocationHelper(context) }

    // Ejecutor para manejar la captura de la foto en el hilo principal
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // ¿Tenemos una foto temporal para revisar?
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }

    // Variable para controlar si mostramos el formulario de multa
    var showTicketForm by remember { mutableStateOf(false) }

    // Verificamos si hay algún proceso activo en pantalla que no sea la cámara en vivo
    val isShowingProcess = sifaViewModel.isLoading ||
            sifaViewModel.detectedPlate != null ||
            sifaViewModel.detectionError != null ||
            coreViewModel.vehicleData != null ||
            capturedPhotoPath != null

    // Interceptamos el botón físico "Atrás" del celular
    BackHandler(enabled = isShowingProcess) {
        // primero eliminamos la foto que se tomó
        ImageUtils.deleteImageFile(capturedPhotoPath)

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
            }
        }
    }

    // INTERCAMBIO DE VISTAS

    if (coreViewModel.submitSuccess) {
        // VISTA DE EXITO AL REGISTRAR INFRACCION
        TicketSuccessScreen(
            onAnimationFinished = {
                // Esto se ejecuta cuando se termina la animación

                // borramos la foto en caché (carpeta evidencia_multas)
                ImageUtils.deleteImageFile(capturedPhotoPath)

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

        // Pedimos la lista de infracciones al servidor la primera vez que se abre esto
        LaunchedEffect(Unit) {
            if (coreViewModel.tiposInfraccion.isEmpty()) {
                coreViewModel.fetchTiposInfraccion()
            }
        }

        TicketScreen(
            vehicleData = coreViewModel.vehicleData!!,
            tiposInfraccion = coreViewModel.tiposInfraccion,
            mainPhotoPath = capturedPhotoPath, // Pasamos la foto de evidencia
            latitude = sifaViewModel.latitude,
            longitude = sifaViewModel.longitude,
            isSubmitting = coreViewModel.isSubmittingInfraccion,
            onCancelClick = { showTicketForm = false }, // Vuelve a la ficha del vehículo
            onSubmitClick = { idInfraccion, observaciones, lat, lon ->
                // Obtenemos la fecha actual en formato ISO 8601
                val fechaActual = java.time.LocalDateTime.now().toString()
                
                // Construimos el objeto que espera el Backend
                val request = com.sifa.sifa_go.data.model.InfraccionCreateRequest(
                    lugar = "Ubicación GPS: $lat, $lon",
                    fecha = fechaActual,
                    latitud = lat?.toFloat() ?: 0f,
                    longitud = lon?.toFloat() ?: 0f,
                    patenteVehiculo = coreViewModel.vehicleData!!.patente,
                    idTipoInfraccion = idInfraccion,
                    observaciones = observaciones,
                    urlsEvidencias = listOf(capturedPhotoPath ?: "evidencia_local_pendiente")
                )
                
                // Disparamos la petición POST
                coreViewModel.submitInfraccion(request)
            }
        )

    } else if (coreViewModel.vehicleData != null) {
        // VISTA DE INFORMACIÓN DEL VEHÍCULO
        VehicleInfoScreen(
            vehicleData = coreViewModel.vehicleData!!,
            onIssueFineClick = {
                showTicketForm = true // se muestra el formulario de la infraccion
            },
            onNewScanClick = {
                // limpiamos la foto física antes de reiniciar el proceso
                ImageUtils.deleteImageFile(capturedPhotoPath)

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
            errorMessage = coreViewModel.errorMessage ?: sifaViewModel.detectionError, // Mostramos error de IA o del Core
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
                ImageUtils.deleteImageFile(capturedPhotoPath)

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

        // VISTA 1: PREVISUALIZACIÓN
        PreviewScreen(
            photoPath = capturedPhotoPath!!,
            onRetakePhoto = {
                // limpiamos la foto física antes de reiniciar el proceso
                ImageUtils.deleteImageFile(capturedPhotoPath)

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
                        takePicture(
                            cameraController,
                            context,
                            mainExecutor,
                        ) { path ->
                            capturedPhotoPath = path
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
                Text("Permiso Denegado", modifier = Modifier.padding(paddingValues))
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