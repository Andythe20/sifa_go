package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.DeviceRegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApiService {
    @POST("/core/api/v1/devices/register")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): Response<Unit>
}

object DeviceRetrofitClient {
    val apiService: DeviceApiService by lazy {
        NetworkModule.retrofit.create(DeviceApiService::class.java)
    }
}
