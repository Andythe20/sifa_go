package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    vehicleData: PlateInfoResponse,
    tiposInfraccion: List<TipoInfraccionResponse>,
    mainPhotoPath: String?, // La foto que ya tomamos al escanear la patente
    onCancelClick: () -> Unit,
    onSubmitClick: (Int, String) -> Unit // Pasa el ID de la infracción y las observaciones
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTipo by remember { mutableStateOf<TipoInfraccionResponse?>(null) }
    var observaciones by remember { mutableStateOf("") }

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
                .padding(bottom = 24.dp)
        )

        // Resumen del Vehículo (Solo lectura rápida para el fiscalizador)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Patente: ${vehicleData.patente ?: "N/A"}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = "Vehículo: ${vehicleData.marca} ${vehicleData.modelo}")
            }
        }

        // 1. DROPDOWN DE TIPO DE INFRACCIÓN
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedTipo?.nombre ?: "Seleccione una infracción...", // Ajusta "descripcion" según tu DTO
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de Infracción *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (tiposInfraccion.isEmpty()) {
                    DropdownMenuItem(text = { Text("Cargando infracciones...") }, onClick = { })
                } else {
                    tiposInfraccion.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo.nombre) }, // Ajusta "descripcion" según tu DTO
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
            onValueChange = { observaciones = it },
            label = { Text("Observaciones (Opcional)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3. SECCIÓN DE FOTOS DE RESPALDO
        Text(
            text = "FOTOS DE RESPALDO",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Foto principal (la que se tomó para leer la patente)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (mainPhotoPath != null) {
                    Icon(Icons.Filled.Image, contentDescription = "Foto capturada", tint = Color.Gray)
                    // TODO: Aquí en el futuro puedes usar la librería 'Coil' para mostrar la imagen real
                }
            }

            // Botón para agregar más fotos
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { /* TODO: Lógica para tomar otra foto */ },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "Agregar foto", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // BOTONES FINALES
        Button(
            onClick = {
                if (selectedTipo != null) {
                    onSubmitClick(selectedTipo!!.id, observaciones)
                }
            },
            enabled = selectedTipo != null, // Solo se activa si eligió una infracción
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
        ) {
            Text("CONFIRMAR Y EMITIR MULTA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("CANCELAR", color = MaterialTheme.colorScheme.error)
        }
    }
}