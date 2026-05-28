package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
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
    authToken: String = "",
    evidencePhotos: List<String>, // La foto que ya tomamos al escanear la patente
    latitude: Double?,
    longitude: Double?,
    isSubmitting: Boolean = false, // Estado que viene desde el ViewModel (bloquea la UI)
    onCancelClick: () -> Unit,
    onSubmitClick: (Int, String, Double?, Double?) -> Unit, // Pasa el ID de la infracción, observaciones y coordenadas
    onAddPhotoClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTipo by remember { mutableStateOf<TipoInfraccionResponse?>(null) }
    var observaciones by remember { mutableStateOf("") }
    var localTiposInfraccion by remember { mutableStateOf<List<TipoInfraccionResponse>>(tiposInfraccion) }
    var isLoading by remember { mutableStateOf(tiposInfraccion.isEmpty()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(tiposInfraccion, authToken) {
        if (tiposInfraccion.isEmpty() && authToken.isNotEmpty()) {
            isLoading = true
            try {
                val response = CoreRetrofitClient.apiService.getAllTipoInfracciones("Bearer $authToken")
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
        Text(
            text = "FOTOS DE RESPALDO (${evidencePhotos.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dibujamos cada foto real de la lista
            items(evidencePhotos) { photoPath ->
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = "Evidencia",
                    contentScale = ContentScale.Crop, // Corta la imagen para llenar el cuadrado
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                )
            }

            // El botón de agregar foto siempre al final
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

        Spacer(modifier = Modifier.height(40.dp))

        // BOTONES FINALES
        Button(
            onClick = {
                // Solo permitimos un clic si no se está enviando ya
                if (selectedTipo != null && !isSubmitting) {
                    onSubmitClick(selectedTipo!!.id, observaciones, latitude, longitude)
                }
            },
            enabled = selectedTipo != null && !isSubmitting, // Desactiva botón visualmente durante el envío
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
