package com.sifa.sifa_go

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.sifa.sifa_go.core.network.NetworkModule
import com.sifa.sifa_go.domain.push.PushToken
import com.sifa.sifa_go.domain.push.PushTokenRepository
import com.sifa.sifa_go.infrastructure.push.repository.SharedPreferencesPushTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SifaApplication : Application() {

    lateinit var pushTokenRepository: PushTokenRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        initNetworkModule()
        initFirebase()
        initPushModule()
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
