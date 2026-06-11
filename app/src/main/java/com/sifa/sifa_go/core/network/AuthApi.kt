package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.ChangePasswordRequest
import com.sifa.sifa_go.data.model.ChangePasswordResponse
import com.sifa.sifa_go.data.model.LoginRequest
import com.sifa.sifa_go.data.model.LoginResponse
import com.sifa.sifa_go.data.model.PasswordRecoveryRequest
import com.sifa.sifa_go.data.model.PasswordRecoveryResponse
import com.sifa.sifa_go.data.model.PasswordResetRequest
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

    @POST("auth/api/v1/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    @POST("auth/api/v1/recovery/request")
    suspend fun requestRecovery(
        @Body request: PasswordRecoveryRequest
    ): Response<PasswordRecoveryResponse>

    @POST("auth/api/v1/recovery/reset")
    suspend fun resetPassword(
        @Body request: PasswordResetRequest
    ): Response<PasswordRecoveryResponse>
}

object AuthRetrofitClient {
    private var _apiService: AuthApiService? = null

    val apiService: AuthApiService
        get() = _apiService ?: NetworkModule.retrofit.create(AuthApiService::class.java)

    fun setApiService(service: AuthApiService) {
        _apiService = service
    }

    fun resetApiService() {
        _apiService = null
    }
}