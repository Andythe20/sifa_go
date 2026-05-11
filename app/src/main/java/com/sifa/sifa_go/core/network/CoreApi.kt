package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.GET
import com.sifa.sifa_go.data.model.InfraccionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path

interface CoreApiService {
    @GET("/core/api/v1/vehiculos/id/{id}")
    suspend fun getPlateInfo(
        // Agregamos el Header para enviar el Token de seguridad
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): PlateInfoResponse

    // para los tipo de infracciones
    @GET("/core/api/v1/tipoInfracciones/id/{id}")
    suspend fun getTipoInfraccion(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): TipoInfraccionResponse

    @GET("/core/api/v1/tipoInfracciones/all")
    suspend fun getAllTipoInfracciones(
        @Header("Authorization") token: String
    ): List<TipoInfraccionResponse>

    // Envia los datos de la multa al Core Service a través del Gateway
    @Multipart
    @POST("/core/api/v1/infracciones")
    suspend fun createInfraccion(
        @Header("Authorization") token: String,
        @Part("infraccion") request: RequestBody, // Los metadatos en JSON
        @Part fotos: List<MultipartBody.Part>     // Los archivos reales
    ): InfraccionResponse
}

object CoreRetrofitClient {
    private const val BASE_URL = "http://192.168.0.11:9000"

    val apiService: CoreApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoreApiService::class.java)
    }
}