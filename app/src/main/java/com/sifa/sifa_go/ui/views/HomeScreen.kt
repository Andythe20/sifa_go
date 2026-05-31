package com.sifa.sifa_go.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sifa.sifa_go.viewmodel.SifaViewModel

@Composable
fun HomeScreen(
    username: String,
    sifaViewModel: SifaViewModel,
    onStartCamera: () -> Unit,
    onStartManual: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Saludo
        Text(
            text = "¡Bienvenido!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "¿Cómo deseas iniciar la fiscalización?",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp),
            textAlign = TextAlign.Center
        )

        // Botón 1: Cámara (Opción principal)
        Button(
            onClick = onStartCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("ESCANEAR PATENTE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botón 2: Manual (Opción secundaria)
        OutlinedButton(
            onClick = onStartManual,
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("INGRESO MANUAL", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}