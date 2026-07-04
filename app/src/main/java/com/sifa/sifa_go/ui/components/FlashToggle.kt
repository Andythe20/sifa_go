package com.sifa.sifa_go.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun FlashToggle(
    isFlashOn: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = { onToggle(!isFlashOn) }, modifier = modifier) {
        Icon(
            imageVector = if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
            contentDescription = if (isFlashOn) "Desactivar flash" else "Activar flash",
            tint = if (isFlashOn) Color.Yellow else Color.White
        )
    }
}
