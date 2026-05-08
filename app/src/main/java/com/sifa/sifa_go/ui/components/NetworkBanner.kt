package com.sifa.sifa_go.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sifa.sifa_go.core.network.NetworkStatus
import com.sifa.sifa_go.ui.theme.ErrorRed

@Composable
fun NetworkBanner(
    networkStatus: NetworkStatus,
    modifier: Modifier = Modifier
) {
    val isConnected = networkStatus is NetworkStatus.Available

    AnimatedVisibility(
        visible = !isConnected,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErrorRed)
                    .padding(top = 40.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.SignalWifiOff,
                    contentDescription = "Sin conexión",
                    tint = Color.White
                )
                Text(
                    text = "Sin conexión a internet",
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}