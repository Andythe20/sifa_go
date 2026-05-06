package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.PlateInfoResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface CoreApiService {
    @GET("plate/{plate}")
    suspend fun getPlateInfo(
        // Agregamos el Header para enviar el Token de seguridad
        @Header("Authorization") token: String,
        @Path("plate") plate: String
    ): PlateInfoResponse
}

object CoreRetrofitClient {
    private const val BASE_URL = "http://10.15.64.34:9000/"

    val apiService: CoreApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoreApiService::class.java)
    }
}