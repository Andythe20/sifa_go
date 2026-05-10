package com.sifa.sifa_go.ui.views

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TicketSuccessScreen(onAnimationFinished: () -> Unit) {
    // Controla si la animación debe empezar
    var isVisible by remember { mutableStateOf(false) }

    // Animación fluida de escala usando físicas de resorte (Spring)
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // Controla cuánto "rebota"
            stiffness = Spring.StiffnessLow                 // Controla la velocidad del rebote
        ),
        label = "escala_icono"
    )

    // El temporizador maestro
    LaunchedEffect(Unit) {
        isVisible = true      // 1. Dispara la animación de entrada
        delay(2500)           // 2. Mantiene la pantalla visible por 2.5 segundos
        isVisible = false     // 3. Dispara la animación de salida (el ícono se encoge)
        delay(400)            // 4. Espera a que termine la animación de salida
        onAnimationFinished() // 5. Avisa a ScannerScreen que ya puede limpiar todo
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Éxito",
                tint = Color(0xFF4CAF50), // Un verde clásico de éxito
                modifier = Modifier.size(120.dp * scale) // Aplicamos la escala animada
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "INFRACCIÓN EMITIDA",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Registro guardado exitosamente",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}