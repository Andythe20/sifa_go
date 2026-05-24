package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.GET
import com.sifa.sifa_go.data.model.InfraccionResponse
import com.sifa.sifa_go.data.model.InfraccionHistoryItem
import com.sifa.sifa_go.data.model.SpringPageResponse
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
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

    @GET("/core/api/v1/infracciones/all")
    suspend fun getInfractionsHistory(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("user") user: String?,
        @Query("page") page: Int = 0,   // Nueva query para controlar qué página pides
        @Query("size") size: Int = 10   // Nueva query para definir cuántos registros traer
    ): Response<SpringPageResponse<InfraccionHistoryItem>> // Mapeado al Wrapper
}

object CoreRetrofitClient {
    private const val BASE_URL = "http://192.168.100.61:9000"

    val apiService: CoreApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoreApiService::class.java)
    }
}