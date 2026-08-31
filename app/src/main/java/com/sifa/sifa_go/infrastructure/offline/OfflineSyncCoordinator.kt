package com.sifa.sifa_go.infrastructure.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sifa.sifa_go.core.network.getNetworkStatus
import com.sifa.sifa_go.core.network.NetworkStatus
import com.sifa.sifa_go.data.local.dao.PendingInfraccionDao
import com.sifa.sifa_go.data.local.mapping.PendingInfraccionMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Observa el estado de conectividad (reutilizando [getNetworkStatus]) y, cuando
 * hay internet, drena la cola de infracciones pendientes en orden FIFO.
 *
 * Se encarga del caso "app en primer plano". Para segundo plano se usa [OfflineSyncWorker].
 */
class OfflineSyncCoordinator(
    private val context: Context,
    private val dao: PendingInfraccionDao
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            context.getNetworkStatus().collectLatest { status ->
                if (status is NetworkStatus.Available) {
                    drainPendingQueue()
                }
            }
        }
    }

    fun stop() {
        scope.coroutineContext[Job]?.cancel()
        started = false
    }

    /**
     * Reenvía todas las infracciones pendientes hasta vaciar la cola o fallar por red.
     * Es pública porque la reutiliza [OfflineSyncWorker] en segundo plano.
     */
    suspend fun drainPendingQueue() {
        withContext(Dispatchers.IO) {
            var next = nextPending()
            while (next != null) {
                // Si ya no hay red, interrumpimos el drenado; se reanudará al reconectar.
                if (!isNetworkAvailable()) return@withContext

                when (val result = OfflineInfraccionSender.send(next.request, next.imagePaths)) {
                    SyncResult.Success -> {
                        dao.markSynced(next.id)
                        // La cola ya envió exitosamente: liberamos las copias de fotos.
                        next.imagePaths.forEach { com.sifa.sifa_go.core.utils.ImageUtils.deleteImageFile(it) }
                    }
                    is SyncResult.PermanentError -> dao.markFailed(next.id, result.failureReason)
                    is SyncResult.TransientNetworkError -> return@withContext
                }
                next = nextPending()
            }
        }
    }

    private suspend fun nextPending(): com.sifa.sifa_go.domain.model.PendingInfraccion? {
        return dao.getPending().firstOrNull()?.let(PendingInfraccionMapper::toDomain)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
