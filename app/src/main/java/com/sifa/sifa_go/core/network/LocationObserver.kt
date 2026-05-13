package com.sifa.sifa_go.core.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

sealed class GpsStatus {
    object Available : GpsStatus()
    object Unavailable : GpsStatus()
}

// Obtiene el estado del GPS
private fun LocationManager.isGpsEnabled(): Boolean =
    isProviderEnabled(LocationManager.GPS_PROVIDER)

fun Context.getGpsStatus(): Flow<GpsStatus> = callbackFlow {

    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun sendCurrentStatus() {
        val status =
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) GpsStatus.Available else GpsStatus.Unavailable
        trySend(status)
    }

    // Escucha los cambios de estado del GPS
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                sendCurrentStatus()
            }
        }
    }

    registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))

    // Estado inicial
    sendCurrentStatus()

    awaitClose {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w("GpsStatus", "Error al remover receiver: ${e.message}")
        }
    }
}.conflate()

@Composable
fun rememberGpsStatus(): State<GpsStatus> {
    val context = LocalContext.current
    val locationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    val initial = if (locationManager.isGpsEnabled()) GpsStatus.Available else GpsStatus.Unavailable
    return context.getGpsStatus().collectAsState(initial = initial)
}