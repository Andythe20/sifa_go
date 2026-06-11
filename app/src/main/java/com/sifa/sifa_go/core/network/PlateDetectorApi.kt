package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.DetectionRootResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// 1. Definimos la ruta de la solicitud
interface SifaApiService {
    @Multipart
    @POST("/plate/api/v1/detect")
    suspend fun detectPlate(
        @Part file: MultipartBody.Part
    ): DetectionRootResponse // Esperamos una lista como respuesta
}

object RetrofitClient {
    private var _apiService: SifaApiService? = null

    val apiService: SifaApiService
        get() = _apiService ?: NetworkModule.retrofit.create(SifaApiService::class.java)

    fun setApiService(service: SifaApiService) {
        _apiService = service
    }

    fun resetApiService() {
        _apiService = null
    }
}