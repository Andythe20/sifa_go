package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Body
import com.sifa.sifa_go.data.model.InfraccionCreateRequest
import com.sifa.sifa_go.data.model.InfraccionResponse
import retrofit2.http.Header
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
    @POST("/core/api/v1/infracciones")
    suspend fun createInfraccion(
        @Header("Authorization") token: String,
        @Body request: InfraccionCreateRequest
    ): InfraccionResponse
}

object CoreRetrofitClient {
    //private const val BASE_URL = "http://10.15.64.34:9000/"
    private const val BASE_URL = "http://192.168.1.6:9000"

    val apiService: CoreApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoreApiService::class.java)
    }
}