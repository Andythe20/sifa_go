package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.LoginRequest
import com.sifa.sifa_go.data.model.LoginResponse
import com.sifa.sifa_go.data.model.UserResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.Path

interface AuthApiService {
    @POST("auth/api/v1/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/api/v1/users/email/{email}")
    suspend fun getUserByEmail(
        @Header("Authorization") token: String,
        @Path("email") email: String
    ): Response<UserResponse>
}

object AuthRetrofitClient {
    // Puerto 8081 para el Fake Auth Service
    // Modificar ip dependiendo a qué red te conectes
    // En local tanto tu móvil como el pc deben estar conectados al mismo wi-fi
    // "http://44.196.188.33"
    private const val BASE_URL = "http://192.168.100.54:9000"

    val apiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}