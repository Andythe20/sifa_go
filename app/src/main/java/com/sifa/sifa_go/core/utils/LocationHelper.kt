package com.sifa.sifa_go.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class LocationHelper(context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun startPrecisionCalibration(onLocationReceived: (Location) -> Unit) {
        // Configuramos 5 actualizaciones rápidas (1 cada segundo)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setMaxUpdates(5) // Se detiene automáticamente tras 5 intentos
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    onLocationReceived(it)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
}