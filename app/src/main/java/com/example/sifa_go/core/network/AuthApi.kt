package com.example.sifa_go.core.network

import com.example.sifa_go.data.model.LoginRequest
import com.example.sifa_go.data.model.LoginResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

object AuthRetrofitClient {
    // Puerto 8081 para el Fake Auth Service
    // Modificar ip dependiendo a qué red te conectes
    // En local tanto tu móvil como el pc deben estar conectados al mismo wi-fi
    private const val BASE_URL = "http://10.15.71.109:8081/"

    val apiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}