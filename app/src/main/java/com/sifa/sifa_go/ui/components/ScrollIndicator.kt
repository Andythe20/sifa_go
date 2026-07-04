package com.sifa.sifa_go.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScrollIndicator(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = scrollState.value < scrollState.maxValue,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val bounce by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -10f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            )
        )
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "Desplazar hacia abajo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(28.dp)
                .offset(y = bounce.dp)
        )
    }
}
