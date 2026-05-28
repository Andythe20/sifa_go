package com.sifa.sifa_go.core.network

import android.content.Context
import com.sifa.sifa_go.core.utils.SessionManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkModule {
    private lateinit var sessionManager: SessionManager

    fun init(context: Context) {
        sessionManager = SessionManager(context)
    }

    val authOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()
    }
}
