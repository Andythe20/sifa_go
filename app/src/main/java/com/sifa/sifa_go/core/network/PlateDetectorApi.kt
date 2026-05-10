package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.DetectionRootResponse
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// 1. Definimos la ruta de la solicitud
interface SifaApiService {
    @Multipart
    @POST("/plate/api/v1/detect")
    suspend fun detectPlate(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): DetectionRootResponse // Esperamos una lista como respuesta
}

// 2. Configuramos el cliente con tu IP actual
object RetrofitClient {
    // IP apuntando al puerto expuesto por el docker
    // Modificar ip dependiendo a qué red te conectes
    // En local tanto tu móvil como el pc deben estar conectados al mismo wi-fi
    private const val BASE_URL = "http://192.168.100.57:9000"

    val apiService: SifaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SifaApiService::class.java)
    }
}