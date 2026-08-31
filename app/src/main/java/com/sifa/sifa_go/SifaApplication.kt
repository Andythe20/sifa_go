package com.sifa.sifa_go

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.sifa.sifa_go.core.network.NetworkModule
import com.sifa.sifa_go.data.local.db.SifaDatabase
import com.sifa.sifa_go.domain.push.PushToken
import com.sifa.sifa_go.domain.push.PushTokenRepository
import com.sifa.sifa_go.infrastructure.offline.OfflineSyncCoordinator
import com.sifa.sifa_go.infrastructure.offline.OfflineSyncWorker
import com.sifa.sifa_go.infrastructure.push.repository.SharedPreferencesPushTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SifaApplication : Application() {

    lateinit var pushTokenRepository: PushTokenRepository
        private set

    lateinit var offlineSyncCoordinator: OfflineSyncCoordinator
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        initNetworkModule()
        initFirebase()
        initPushModule()
        initOfflineQueue()
        createNotificationChannel()
        retrieveFcmToken()
    }

    private fun initNetworkModule() {
        NetworkModule.init(this)
    }

    private fun initFirebase() {
        FirebaseApp.initializeApp(this)
    }

    private fun initPushModule() {
        pushTokenRepository = SharedPreferencesPushTokenRepository(this)
    }

    /**
     * Inicializa la cola offline: arranca el coordinador que drena las infracciones
     * pendientes al reconectar (foreground) y programa el worker de segundo plano
     * que sirve de respaldo cuando la app no está en primer plano.
     */
    private fun initOfflineQueue() {
        val dao = SifaDatabase.getInstance(this).pendingInfraccionDao()
        offlineSyncCoordinator = OfflineSyncCoordinator(this, dao)
        offlineSyncCoordinator.start()
        OfflineSyncWorker.schedule(this)
    }

    private fun createNotificationChannel() {
        com.sifa.sifa_go.infrastructure.push.notification.NotificationHelper.createNotificationChannel(this)
    }

    private fun retrieveFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM token: $token")
                applicationScope.launch {
                    pushTokenRepository.saveToken(PushToken(token))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to retrieve FCM token", e)
            }
    }

    companion object {
        private const val TAG = "SifaApp"
        lateinit var instance: SifaApplication
            private set
    }
}
