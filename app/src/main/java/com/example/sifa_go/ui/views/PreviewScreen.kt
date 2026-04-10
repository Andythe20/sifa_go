package com.example.sifa_go.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun PreviewScreen(
    photoPath: String,
    onRetakePhoto: () -> Unit,
    onSendPhoto: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Texto de instrucción
        Text(text = "Revisa la captura", modifier = Modifier.padding(top = 16.dp))

        // Imagen previsualizada usando Coil
        AsyncImage(
            model = File(photoPath),
            contentDescription = "Previsualización de patente",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Ocupa el espacio disponible
                .padding(vertical = 16.dp),
            contentScale = ContentScale.Fit
        )

        // Botones de acción
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(onClick = onRetakePhoto) {
                Text("Volver a intentar")
            }

            Button(
                onClick = { onSendPhoto(photoPath) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Color verde para aprobar
            ) {
                Text("Enviar a procesar")
            }
        }
    }
}