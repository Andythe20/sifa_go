package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.LoginRequest
import com.sifa.sifa_go.data.model.LoginResponse
import com.sifa.sifa_go.data.model.RefreshTokenRequest
import com.sifa.sifa_go.data.model.UserResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {
    @POST("auth/api/v1/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/api/v1/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<LoginResponse>

    @GET("auth/api/v1/users/email/{email}")
    suspend fun getUserByEmail(
        @Path("email") email: String
    ): Response<UserResponse>
}

object AuthRetrofitClient {
    private const val BASE_URL = "http://3.219.255.24"

    val apiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(NetworkModule.authOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}