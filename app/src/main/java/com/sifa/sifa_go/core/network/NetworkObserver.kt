package com.sifa.sifa_go.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Unavailable : NetworkStatus()
}

fun Context.getNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(NetworkStatus.Available)
        }

        override fun onLost(network: Network) {
            trySend(NetworkStatus.Unavailable)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            trySend(if (hasInternet) NetworkStatus.Available else NetworkStatus.Unavailable)
        }
    }

    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(networkRequest, callback)

    val currentNetwork = connectivityManager.activeNetwork
    val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
    val initialStatus = if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
        NetworkStatus.Available
    } else {
        NetworkStatus.Unavailable
    }
    trySend(initialStatus)

    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}.conflate()

@Composable
fun rememberNetworkStatus(): State<NetworkStatus> {
    val context = LocalContext.current
    return context.getNetworkStatus().collectAsState(initial = NetworkStatus.Available)
}