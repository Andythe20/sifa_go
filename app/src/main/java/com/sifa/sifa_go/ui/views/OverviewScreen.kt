package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifa.sifa_go.ui.components.ErrorView
import com.sifa.sifa_go.viewmodel.SifaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(sifaViewModel: SifaViewModel) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFormat.format(Date())

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentTime = remember { mutableStateOf(timeFormat.format(Date())) }
    var isRefreshing by remember { mutableStateOf(false) }

    var sessionDuration by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        sifaViewModel.loadInfractionsHistory(today)
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = timeFormat.format(Date())
            kotlinx.coroutines.delay(30_000)
        }
    }

    LaunchedEffect(Unit) {
        val iat = sifaViewModel.getSessionIat()
        if (iat > 0) {
            while (true) {
                val elapsed = System.currentTimeMillis() / 1000 - iat
                val hours = (elapsed / 3600).toInt()
                val minutes = ((elapsed % 3600) / 60).toInt()
                val secs = (elapsed % 60).toInt()
                sessionDuration = "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
                kotlinx.coroutines.delay(1000)
            }
        }
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
            text = "RESUMEN",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                sifaViewModel.loadInfractionsHistory(today)
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            GreetingCard(
                username = sifaViewModel.currentUsername,
                sessionDuration = sessionDuration,
                currentTime = currentTime.value,
                currentDate = today
            )

            LocationCard(
                latitude = sifaViewModel.latitude,
                longitude = sifaViewModel.longitude,
                accuracy = sifaViewModel.gpsAccuracy,
                address = sifaViewModel.currentAddress,
                isCalibrating = sifaViewModel.isGPSCalibrating,
                onRefresh = { sifaViewModel.refreshCurrentLocation() }
            )

            Text(
                text = "Infracciones hoy",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            when {
                sifaViewModel.historyLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                sifaViewModel.historyError != null -> {
                    ErrorView(
                        error = sifaViewModel.historyError,
                        onRetry = { sifaViewModel.loadInfractionsHistory(today) }
                    )
                }

                else -> {
                    val stats = remember(sifaViewModel.infractionsHistory) {
                        computeStats(sifaViewModel.infractionsHistory)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "En proceso",
                            value = "${stats.enProceso}",
                            icon = Icons.Filled.HourglassEmpty,
                            color = Color(0xFFFF5722),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Completadas",
                            value = "${stats.completadas}",
                            icon = Icons.Filled.CheckCircle,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Canceladas",
                            value = "${stats.canceladas}",
                            icon = Icons.Filled.Cancel,
                            color = Color(0xFFF11A00),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (stats.total > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total del día",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${stats.total}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aún no registras infracciones hoy",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GreetingCard(
    username: String,
    sessionDuration: String?,
    currentTime: String,
    currentDate: String
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Buenos días"
        hour < 18 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$greeting,",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = username.ifBlank { "Fiscalizador" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentTime,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
                Text(
                    text = formatoFechaLegible(currentDate),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }

            if (sessionDuration != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sesión activa: $sessionDuration",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    latitude: Double?,
    longitude: Double?,
    accuracy: Float?,
    address: String?,
    isCalibrating: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCalibrating) { onRefresh() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCalibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = if (latitude != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Ubicación actual",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (latitude != null && longitude != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (accuracy != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val accuracyColor = when {
                            accuracy <= 10f -> Color(0xFF4CAF50)
                            accuracy <= 50f -> Color(0xFFFF9800)
                            else -> Color(0xFFF11A00)
                        }
                        Text(
                            text = "±${accuracy.toInt()}m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accuracyColor
                        )
                    }
                }
                if (!address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = address,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            } else {
                Text(
                    text = "Presiona para obtener tu ubicación",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatoFechaLegible(dateStr: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val output = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "CL"))
        output.format(input.parse(dateStr))
    } catch (e: Exception) {
        dateStr
    }
}

private data class InfractionStats(
    val total: Int,
    val enProceso: Int,
    val completadas: Int,
    val canceladas: Int
)

private fun computeStats(infractions: List<com.sifa.sifa_go.data.model.InfraccionHistoryItem>): InfractionStats {
    var enProceso = 0
    var completadas = 0
    var canceladas = 0

    for (inf in infractions) {
        when (inf.status?.lowercase()) {
            "en proceso", "pending" -> enProceso++
            "exported", "completado" -> completadas++
            "cancelada", "rejected" -> canceladas++
        }
    }

    return InfractionStats(
        total = infractions.size,
        enProceso = enProceso,
        completadas = completadas,
        canceladas = canceladas
    )
}
