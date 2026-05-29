package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.sifa.sifa_go.data.model.InfraccionHistoryItem
import com.sifa.sifa_go.ui.components.PaginationBar
import com.sifa.sifa_go.viewmodel.SifaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sifaViewModel: SifaViewModel
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFormat.format(Date())
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sifaViewModel.loadInfractionsHistory(today)
    }

    LaunchedEffect(sifaViewModel.historyLoading) {
        if (!sifaViewModel.historyLoading && isRefreshing) {
            isRefreshing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "INFRACCIONES EMITIDAS",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fecha: $today",
                color = Color.Gray,
                fontSize = 14.sp
            )

            if (!sifaViewModel.historyLoading && sifaViewModel.infractionsHistory.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total: ${sifaViewModel.totalElements}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    if (sifaViewModel.totalPages > 1) {
                        Text(
                            text = "  |  ",
                            color = Color.Gray.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        PaginationBar(
                            currentPage = sifaViewModel.currentPage + 1,
                            totalPages = sifaViewModel.totalPages,
                            isFirstPage = sifaViewModel.isFirstPage,
                            isLastPage = sifaViewModel.isLastPage,
                            onPreviousPage = {
                                sifaViewModel.loadInfractionsHistory(today, sifaViewModel.currentPage - 1)
                            },
                            onNextPage = {
                                sifaViewModel.loadInfractionsHistory(today, sifaViewModel.currentPage + 1)
                            }
                        )
                    }
                }
            }
        }

Spacer(modifier = Modifier.height(16.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                sifaViewModel.loadInfractionsHistory(today)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                sifaViewModel.historyLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                sifaViewModel.historyError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = sifaViewModel.historyError ?: "Error desconocido",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { sifaViewModel.loadInfractionsHistory(today) }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                sifaViewModel.infractionsHistory.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No has registrado infracciones hoy",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Las infracciones que crees aparecerán aquí",
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sifaViewModel.infractionsHistory) { infraction ->
                            InfractionHistoryCard(infraction = infraction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfractionHistoryCard(infraction: InfraccionHistoryItem) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (infraction.status?.lowercase()) {
        "en proceso", "pending" -> Color(0xFFFF5722)
        "exported", "completado", -> Color(0xFF4CAF50)
        "cancelada", "rejected" -> Color(0xFFF11A00)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ID: ${infraction.id ?: "N/A"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = infraction.vehicle?.plate ?: "N/A",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = infraction.status.uppercase(),
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!infraction.tipoInfraccion?.nombre.isNullOrBlank()) {
                        Box(
                            modifier = Modifier.width(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = infraction.tipoInfraccion?.nombre ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = formatTimestamp(infraction.fecha?: "N/A"),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.width(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = buildString {
                            append(infraction.vehicle?.brand ?: "")
                            if (!infraction.vehicle?.model.isNullOrBlank()) {
                                append(" ")
                                append(infraction.vehicle?.model)
                            }
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!infraction.vehicle?.color.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = infraction.vehicle?.color ?: "",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.width(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            val count = infraction.evidenceUrls?.size ?: 0
                            append(count)
                            append(if (count == 1) " foto" else " fotos")
                        },
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                ExpandChevron(expanded = expanded)
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Denunciado: ${infraction.propietario?.nombreCompleto ?: "N/A"}",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Policy,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RUT: ${infraction.propietario?.rut ?: "N/A"}",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = infraction.location?.address ?: "Sin dirección",
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!infraction.tipoInfraccion?.disposicionInfringida.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Disposición: ${infraction.tipoInfraccion?.disposicionInfringida}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (!infraction.observaciones.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Observaciones: ${infraction.observaciones}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                val allImages = buildList {
                    infraction.evidenceUrls?.let { addAll(it) }
                }.distinct()

                if (allImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Evidencia (${allImages.size}):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ImageGallery(
                        images = allImages,
                        statusColor = statusColor
                    )
                }
            }

        }
    }
}

@Composable
private fun ExpandChevron(expanded: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        shadowElevation = 1.dp
    ) {
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = if (expanded) "Contraer" else "Expandir",
            tint = Color.Gray,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .rotate(if (expanded) 90f else 0f)
                .padding(6.dp)
                .size(18.dp)
        )
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timestamp)
        date?.let { outputFormat.format(it) } ?: timestamp
    } catch (e: Exception) {
        timestamp
    }
}

/**
 * Galeria de miniaturas de evidencia. Muestra hasta 3 imagenes en fila,
 * con overlay "+N" si hay mas. Al hacer clic abre [ImageViewerDialog].
 * Cada miniatura muestra un spinner mientras se descarga.
 */
@Composable
fun ImageGallery(
    images: List<String>,
    statusColor: Color
) {
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        images.take(3).forEachIndexed { index, imageUrl ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedImageIndex = index }
                    .background(Color.LightGray)
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "Evidencia ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Error al cargar",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
                if (images.size > 3 && index == 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${images.size - 3}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        if (images.size < 3) {
            repeat(3 - images.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    selectedImageIndex?.let { currentIndex ->
        ImageViewerDialog(
            images = images,
            initialIndex = currentIndex,
            statusColor = statusColor,
            onDismiss = { selectedImageIndex = null }
        )
    }
}

/**
 * Dialogo a pantalla completa para visualizar imagenes de evidencia
 * con navegacion entre ellas y cierre. Muestra un spinner en cada
 * imagen mientras se descarga.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerDialog(
    images: List<String>,
    initialIndex: Int,
    statusColor: Color,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { images.size }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        currentIndex = pagerState.currentPage
    }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val backgroundAlpha by remember {
        derivedStateOf {
            (0.95f - (dragOffsetY / 600f).coerceIn(0f, 0.95f))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        },
                        onDragEnd = {
                            if (dragOffsetY > 200f) {
                                onDismiss()
                            } else {
                                dragOffsetY = 0f
                            }
                        },
                        onDragCancel = { dragOffsetY = 0f }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                    .fillMaxSize()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    SubcomposeAsyncImage(
                        model = images[page],
                        contentDescription = "Imagen ${page + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Error al cargar",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .statusBarsPadding()
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (images.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newPage = if (pagerState.currentPage > 0) {
                                    pagerState.currentPage - 1
                                } else {
                                    images.size - 1
                                }
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(newPage)
                                }
                            },
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                Icons.Filled.ChevronLeft,
                                contentDescription = "Anterior",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = {
                                val newPage = if (pagerState.currentPage < images.size - 1) {
                                    pagerState.currentPage + 1
                                } else {
                                    0
                                }
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(newPage)
                                }
                            },
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = "Siguiente",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${images.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
