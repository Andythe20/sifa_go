package com.sifa.sifa_go.infrastructure.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.sifa.sifa_go.data.local.db.SifaDatabase
import java.util.concurrent.TimeUnit

/**
 * Worker de segundo plano que reenvía las infracciones pendientes de la cola offline.
 * Se activa solo cuando hay conexión a internet (restricción de WorkManager) y sirve
 * de respaldo al [OfflineSyncCoordinator] para cuando la app está en segundo plano o cerrada.
 */
class OfflineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = SifaDatabase.getInstance(applicationContext)
        val coordinator = OfflineSyncCoordinator(applicationContext, db.pendingInfraccionDao())
        coordinator.drainPendingQueue()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "offline_infraccion_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
