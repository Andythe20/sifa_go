package com.example.sifa_go.ui.views

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
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
import com.example.sifa_go.viewmodel.CoreViewModel
import com.example.sifa_go.viewmodel.SifaViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    // Ejecutor para manejar la captura de la foto en el hilo principal
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // ¿Tenemos una foto temporal para revisar?
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    // INTERCAMBIO DE VISTAS
    if (sifaViewModel.isLoading) {
        // VISTA DE CARGA
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Procesando imagen con IA...", modifier = Modifier.padding(top = 60.dp))
        }
    }  else if (coreViewModel.vehicleData != null) {
        // VISTA DE INFORMACIÓN DEL VEHÍCULO
        VehicleInfoScreen(
            vehicleData = coreViewModel.vehicleData!!,
            onIssueFineClick = {
                // TODO: Aquí navegaremos al formulario de multa más adelante
                println("Ir al formulario de multa...")
            },
            onNewScanClick = {
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
            errorMessage = sifaViewModel.detectionError ?: coreViewModel.errorMessage, // Mostramos error de IA o del Core
            isLoading = coreViewModel.isLoading, // Le pasamos el estado de carga del Core API
            onConsultClick = { finalPlate ->
                coreViewModel.fetchVehicleInfo(finalPlate)
            },
            onRetakePhoto = {
                capturedPhotoPath = null
                sifaViewModel.clearProcess()
                coreViewModel.clearData()
            }
        )

        // Si el CoreViewModel está cargando, mostramos un feedback
        if (coreViewModel.isLoading) {
            Text("Consultando base de datos nacional...")
        }
        // Si devolvió datos, los imprimimos temporalmente (luego haremos una vista linda para esto)
        if (coreViewModel.vehicleData != null) {
            Text("Vehículo: ${coreViewModel.vehicleData?.marca} ${coreViewModel.vehicleData?.modelo}")
        }
    } else if (capturedPhotoPath != null) {

        // VISTA 1: PREVISUALIZACIÓN
        PreviewScreen(
            photoPath = capturedPhotoPath!!,
            onRetakePhoto = {
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
            if (permissionState.status.isGranted) {
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
    val persistentFolder = File(context.filesDir, "evidencia_multas")
    if (!persistentFolder.exists()) persistentFolder.mkdirs()

    val permanentFile = File(persistentFolder, "patente_${System.currentTimeMillis()}.jpg")
    tempFile.copyTo(permanentFile, overwrite = true)

    return permanentFile.absolutePath
}