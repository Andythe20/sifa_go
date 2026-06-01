package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.LoginRequest
import com.sifa.sifa_go.data.model.LoginResponse
import com.sifa.sifa_go.data.model.RefreshTokenRequest
import com.sifa.sifa_go.data.model.UserResponse
import retrofit2.Response
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
    val apiService: AuthApiService by lazy {
        NetworkModule.retrofit.create(AuthApiService::class.java)
    }
}