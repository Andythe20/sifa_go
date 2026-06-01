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

// 2. Configuramos el cliente con tu IP actual
object RetrofitClient {
    val apiService: SifaApiService by lazy {
        NetworkModule.retrofit.create(SifaApiService::class.java)
    }
}