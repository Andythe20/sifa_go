package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlateResultScreen(
    initialPlate: String?,
    errorMessage: String?,
    isLoading: Boolean,
    isManualEntry: Boolean = false,
    onConsultClick: (String) -> Unit,
    onRetakePhoto: () -> Unit
) {
    // Estado local para permitir al usuario modificar la patente
    var currentPlate by remember { mutableStateOf(initialPlate ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // --- PARTE SUPERIOR / MEDIO: Formulario y Feedback ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Empuja los botones hacia abajo
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Si hay error (ej. IA no detectó nada o falló la red)
            if (errorMessage != null) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Alerta",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Por favor, digite la patente manualmente:",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else if (isManualEntry) {
                // TEXTOS PARA INGRESO MANUAL VOLUNTARIO
                Text(
                    text = "Ingreso Manual",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Digite la patente del vehículo",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

            } else {
                // Si fue exitoso
                Text(
                    text = "Patente Detectada",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Verifique o corrija el texto si es necesario",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de texto de la patente (Estilizado para que resalte)
            OutlinedTextField(
                value = currentPlate,
                onValueChange = {
                    // Limitamos a 6 caracteres (ej: GKSB78) y forzamos mayúsculas
                    if (it.length <= 6) currentPlate = it.uppercase()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                textStyle = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(0.8f) // Que no ocupe todo el ancho para que parezca una placa
            )
        }

        // --- PARTE INFERIOR: Botones de Acción ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (currentPlate.length >= 5) { // Validación mínima
                        onConsultClick(currentPlate)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                enabled = currentPlate.length >= 5 && !isLoading // Desactiva el botón si está vacío o incompleto o si está cargando
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(2.dp))
                } else {
                    Text("CONSULTAR DATOS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRetakePhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading // Desactiva el botón si está cargando
            ) {
                Text(if (isManualEntry) "Volver a la cámara" else "Tomar otra fotografía")
            }
        }
    }
}