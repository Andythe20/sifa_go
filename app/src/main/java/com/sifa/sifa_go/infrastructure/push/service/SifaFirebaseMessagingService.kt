package com.sifa.sifa_go.infrastructure.push.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sifa.sifa_go.domain.push.PushToken
import com.sifa.sifa_go.infrastructure.push.notification.NotificationHelper
import com.sifa.sifa_go.infrastructure.push.repository.SharedPreferencesPushTokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SifaFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var tokenRepository: SharedPreferencesPushTokenRepository

    override fun onCreate() {
        super.onCreate()
        tokenRepository = SharedPreferencesPushTokenRepository(applicationContext)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: $token")
        scope.launch {
            tokenRepository.saveToken(PushToken(token))
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        if (title != null && body != null) {
            NotificationHelper.showNotification(applicationContext, title, body)
        }
        Log.d(TAG, "Message received: data=${message.data}")
    }

    companion object {
        private const val TAG = "SifaFCM"
    }
}
