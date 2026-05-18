package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun PreviewScreen(
    photoPath: String,
    onRetakePhoto: () -> Unit,
    onSendPhoto: (String) -> Unit
) {

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0288D1), Color(0xFF01579B)) // Ejemplo de azul
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Texto de instrucción
        Text(
            text = "Revisa que la captura sea correcta",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium)

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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onRetakePhoto,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp) // Más cuadrado
            ) {
                Text("Reintentar")
            }

            Button(
                onClick = { onSendPhoto(photoPath) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Procesar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}