package com.sifa.sifa_go.ui.views

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.sifa.sifa_go.core.image.ImageSanitizer
import com.sifa.sifa_go.core.network.GpsStatus
import com.sifa.sifa_go.core.utils.takePictureWithFlash
import com.sifa.sifa_go.core.utils.vibrateShort
import com.sifa.sifa_go.ui.components.FlashToggle
import com.sifa.sifa_go.viewmodel.SifaViewModel
import java.io.File
import java.util.concurrent.Executor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveScannerScreen(
    cameraController: LifecycleCameraController,
    gpsStatus: GpsStatus,
    isGPSCalibrating: Boolean,
    gpsAccuracy: Float?,
    onStartGpsCalibration: () -> Unit,
    onPhotoTaken: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    var isFlashOn by remember { mutableStateOf(false) }

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onStartGpsCalibration()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (permissionState.allPermissionsGranted) {
                ExtendedFloatingActionButton(
                    onClick = {
                        context.vibrateShort()
                        takePicture(cameraController, context, mainExecutor, isFlashOn) { path ->
                            onPhotoTaken(path)
                        }
                    },
                    containerColor = Color.White,
                    contentColor = Color.Blue,
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Cámara") },
                    text = { Text("TOMAR FOTO") }
                )
            }
        }
    ) { paddingValues ->
        if (permissionState.allPermissionsGranted) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                CameraView(cameraController = cameraController, lifecycle = lifecycle, modifier = Modifier.fillMaxSize())
                ScannerOverlay()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FlashToggle(
                        isFlashOn = isFlashOn,
                        onToggle = { isFlashOn = it },
                    )

                    val isGpsAvailable = gpsStatus is GpsStatus.Available
                    Row(
                        modifier = Modifier.clickable(enabled = isGpsAvailable && !isGPSCalibrating) {
                            onStartGpsCalibration()
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isGPSCalibrating -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            !isGpsAvailable -> Icon(Icons.Default.LocationOff, contentDescription = "GPS desactivado", tint = Color.Red, modifier = Modifier.size(16.dp))
                            else -> gpsAccuracy?.let { accuracy ->
                                Icon(Icons.Default.MyLocation, contentDescription = "Recalibrar GPS", tint = if (accuracy < 10f) Color.Green else Color.Yellow, modifier = Modifier.size(16.dp))
                            }
                        }
                        val accuracyText = if (!isGpsAvailable) " GPS: --" else gpsAccuracy?.let { " GPS: ${it.toInt()}m" } ?: " GPS: --"
                        val accuracyColor = when {
                            !isGpsAvailable -> Color.Red
                            gpsAccuracy?.let { it < 10f } == true -> Color.Green
                            else -> Color.Yellow
                        }
                        Text(text = accuracyText, color = accuracyColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else {
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

@Composable
fun CameraView(
    cameraController: LifecycleCameraController,
    lifecycle: LifecycleOwner,
    modifier: Modifier = Modifier,
    showZoomHint: Boolean = true
) {
    val zoomLevels = remember { listOf(1.0f, 2.0f, 3.0f) }
    val zoomState by cameraController.zoomState.observeAsState()
    val currentZoom = zoomState?.zoomRatio ?: 1.0f

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    controller = cameraController
                }
            }
        )

        if (showZoomHint) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .clickable {
                        val nextZoom = when {
                            currentZoom < 1.5f -> 2.0f
                            currentZoom < 2.5f -> 3.0f
                            else -> 1.0f
                        }
                        cameraController.setZoomRatio(nextZoom)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentZoom.toInt()}x",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun takePicture(
    cameraController: LifecycleCameraController,
    context: Context,
    executor: Executor,
    useFlash: Boolean = false,
    onPhotoTaken: (String) -> Unit
) {
    val photoFile = File(context.cacheDir, "sifa_photo_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    cameraController.takePictureWithFlash(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val permanentPath = ImageSanitizer.sanitize(photoFile).absolutePath
                if (photoFile.exists()) {
                    photoFile.delete()
                }
                onPhotoTaken(permanentPath)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("LiveScannerScreen", "Error al tomar la foto", exception)
            }
        },
        enabled = useFlash
    )
}

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val rectWidth = canvasWidth * 0.85f
            val rectHeight = rectWidth * 0.4f

            val left = (canvasWidth - rectWidth) / 2
            val top = (canvasHeight - rectHeight) / 2
            val right = left + rectWidth
            val bottom = top + rectHeight

            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(12.dp.toPx())
            )

            val cornerLength = 80f
            val cornerPath = Path().apply {
                moveTo(left, top + cornerLength)
                lineTo(left, top)
                lineTo(left + cornerLength, top)
                moveTo(right - cornerLength, top)
                lineTo(right, top)
                lineTo(right, top + cornerLength)
                moveTo(right, bottom - cornerLength)
                lineTo(right, bottom)
                lineTo(right - cornerLength, bottom)
                moveTo(left + cornerLength, bottom)
                lineTo(left, bottom)
                lineTo(left, bottom - cornerLength)
            }

            drawPath(
                path = cornerPath,
                color = Color.White,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

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
