package com.sifa.sifa_go.core.network

import android.content.Context
import com.sifa.sifa_go.core.utils.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private lateinit var sessionManager: SessionManager

    fun init(context: Context) {
        sessionManager = SessionManager(context)
    }

    val authOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(ServerConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ServerConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ServerConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionManager))
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ServerConfig.BASE_URL)
            .client(authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
