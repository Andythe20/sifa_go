package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import com.sifa.sifa_go.core.network.CoreRetrofitClient
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import com.sifa.sifa_go.ui.theme.SIFA_GOTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    vehicleData: PlateInfoResponse,
    tiposInfraccion: List<TipoInfraccionResponse> = emptyList(),
    evidencePhotos: List<String>, // La foto que ya tomamos al escanear la patente
    latitude: Double?,
    longitude: Double?,
    isSubmitting: Boolean = false, // Estado que viene desde el ViewModel (bloquea la UI)
    isManualEntry: Boolean = false, // Identifica si venimos de ingreso manual
    onCancelClick: () -> Unit,
    onSubmitClick: (Int, String, Double?, Double?) -> Unit, // Pasa el ID de la infracción, observaciones y coordenadas
    onAddPhotoClick: () -> Unit,
    onRemovePhoto: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTipo by remember { mutableStateOf<TipoInfraccionResponse?>(null) }
    var observaciones by remember { mutableStateOf("") }
    var localTiposInfraccion by remember { mutableStateOf<List<TipoInfraccionResponse>>(tiposInfraccion) }
    var isLoading by remember { mutableStateOf(tiposInfraccion.isEmpty()) }
    var fullscreenImagePath by remember { mutableStateOf<String?>(null) }
    var removeConfirmIndex by remember { mutableIntStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tiposInfraccion) {
        if (tiposInfraccion.isEmpty()) {
            isLoading = true
            try {
                val response = CoreRetrofitClient.apiService.getAllTipoInfracciones()
                if (response.isSuccessful) {
                    localTiposInfraccion = response.body()?.content ?: emptyList()
                } else {
                    println("Error del servidor al cargar tipos en UI: ${response.code()}")
                    localTiposInfraccion = emptyList()
                }
            } catch (e: Exception) {
                println("Error cargando tipos de infraccion: $e")
            } finally {
                isLoading = false
            }
        } else if (tiposInfraccion.isNotEmpty()) {
            localTiposInfraccion = tiposInfraccion
        }
    }

    val displayTiposInfraccion = if (localTiposInfraccion.isNotEmpty()) localTiposInfraccion else tiposInfraccion

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
        unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary,
        selectionColors = TextSelectionColors(
            handleColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Text(
            text = "CURSAR INFRACCIÓN",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 24.dp)
        )

        // Resumen del Vehículo (Solo lectura rápida para el fiscalizador)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Patente: ${vehicleData.patente}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Vehículo: ${vehicleData.marca} ${vehicleData.modelo}",
                    color = MaterialTheme.colorScheme.primary
                )

                if (latitude != null && longitude != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ubicación GPS: $latitude, $longitude",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // 1. DROPDOWN DE TIPO DE INFRACCIÓN
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (!isSubmitting) expanded = !expanded }, // Bloqueamos si está enviando
            modifier = Modifier
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTipo?.nombre ?: "Seleccione una infracción...",
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de Infracción *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onPrimary)
            ) {
                if (displayTiposInfraccion.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(if (isLoading) "Cargando infracciones..." else "No hay infracciones disponibles") },
                        onClick = { }
                    )
                } else {
                    displayTiposInfraccion.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo.nombre, color = MaterialTheme.colorScheme.onBackground) },
                            onClick = {
                                selectedTipo = tipo
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. OBSERVACIONES (Opcional)
        OutlinedTextField(
            value = observaciones,
            onValueChange = { if (!isSubmitting) observaciones = it }, // Bloqueamos edición durante envío
            label = { Text("Observaciones (Opcional)") },
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4,
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. SECCIÓN DE FOTOS DE RESPALDO
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(
                text = "FOTOS DE RESPALDO (${evidencePhotos.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (isManualEntry && evidencePhotos.isEmpty()) {
                Text(
                    text = "* Se requiere al menos 1 foto para el ingreso manual",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(evidencePhotos.size) { index ->
                val photoPath = evidencePhotos[index]
                Box(modifier = Modifier.size(80.dp)) {
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = "Evidencia",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { fullscreenImagePath = photoPath }
                    )
                    if (index > 0 && onRemovePhoto != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { removeConfirmIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Eliminar foto",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable(enabled = !isSubmitting) { onAddPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AddAPhoto,
                            contentDescription = "Agregar foto",
                            tint = if (isSubmitting) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Visor de pantalla completa con carrusel
        fullscreenImagePath?.let { _ ->
            val initialPage = evidencePhotos.indexOf(fullscreenImagePath).coerceAtLeast(0)
            FullScreenPhotoViewer(
                photos = evidencePhotos,
                initialPage = initialPage,
                onDismiss = { fullscreenImagePath = null },
                onRemoveRequest = { index ->
                    removeConfirmIndex = index
                }
            )
        }

        // Diálogo de confirmación para eliminar foto
        if (removeConfirmIndex in evidencePhotos.indices) {
            AlertDialog(
                onDismissRequest = { removeConfirmIndex = -1 },
                title = { Text("Eliminar foto") },
                text = { Text("¿Estás seguro de eliminar esta foto de respaldo?") },
                confirmButton = {
                    TextButton(onClick = {
                        if (removeConfirmIndex in evidencePhotos.indices) {
                            onRemovePhoto?.invoke(evidencePhotos[removeConfirmIndex])
                        }
                        removeConfirmIndex = -1
                    }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { removeConfirmIndex = -1 }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // BOTONES FINALES
        val canSubmit = selectedTipo != null && !isSubmitting && (!isManualEntry || evidencePhotos.isNotEmpty())

        Button(
            onClick = {
                if (canSubmit) {
                    showConfirmDialog = true
                }
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("PROCESANDO...")
            } else {
                Text("CONFIRMAR Y EMITIR MULTA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Diálogo de confirmación de emisión de multa
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirmar emisión de multa") },
                text = {
                    Column {
                        Text("¿Estás seguro de emitir esta multa?")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Patente: ${vehicleData.patente}",
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedTipo != null) {
                            Text(
                                text = "Infracción: ${selectedTipo!!.nombre}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            onSubmitClick(selectedTipo!!.id, observaciones, latitude, longitude)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Sí, emitir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancelClick,
            enabled = !isSubmitting, // No puede cancelar si ya se está enviando
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("CANCELAR", color = if (isSubmitting) Color.Gray else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FullScreenPhotoViewer(
    photos: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
    onRemoveRequest: (Int) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val scope = rememberCoroutineScope()
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 250f

    LaunchedEffect(initialPage) {
        pagerState.scrollToPage(initialPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragOffsetY > dismissThreshold) {
                                onDismiss()
                            }
                            dragOffsetY = 0f
                        },
                        onDragCancel = { dragOffsetY = 0f }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) { page ->
                    AsyncImage(
                        model = File(photos[page]),
                        contentDescription = "Foto evidencia",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Close button — TopStart
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                }

                // Delete button — TopEnd with same size/padding as close
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = { onRemoveRequest(pagerState.currentPage) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Eliminar foto",
                            tint = Color(0xFFEF5350)
                        )
                    }
                }

                // Thumbnail carousel
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(photos.size) { index ->
                            val isCurrent = index == pagerState.currentPage
                            Box(modifier = Modifier.size(48.dp)) {
                                AsyncImage(
                                    model = File(photos[index]),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .border(
                                            width = if (isCurrent) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                )
                            }
                        }
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} / ${photos.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
