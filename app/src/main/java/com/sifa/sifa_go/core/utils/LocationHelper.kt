package com.sifa.sifa_go.core.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @android.annotation.SuppressLint("MissingPermission")
    fun startPrecisionCalibration(onLocationReceived: (Location) -> Unit) {
        // Configuración de actualizaciones de alta precisión
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setMaxUpdates(5)
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

    /**
     * Obtiene una dirección legible. Maneja la depreciación de getFromLocation según la API.
     */
    fun getAddressFromLocation(lat: Double, lon: Double, onResult: (String?) -> Unit) {
        val geocoder = Geocoder(context, Locale.getDefault())

        // Versión para API 33 o superior (Uso de Callback asíncrono)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val addressText = addresses.firstOrNull()?.getAddressLine(0)
                    onResult(addressText)
                }
                override fun onError(errorMessage: String?) {
                    Log.e("LocationHelper", "Error en Geocoding: $errorMessage")
                    onResult(null)
                }
            })
        } else {
            // Versión para APIs antiguas (Ejecución manual en hilo secundario)
            Thread {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val addressText = addresses?.firstOrNull()?.getAddressLine(0)
                    onResult(addressText)
                } catch (e: Exception) {
                    Log.e("LocationHelper", "Error en Geocoding antiguo", e)
                    onResult(null)
                }
            }.start()
        }
    }
}