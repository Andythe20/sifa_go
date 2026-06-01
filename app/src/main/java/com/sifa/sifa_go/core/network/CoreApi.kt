package com.sifa.sifa_go.core.network

import com.sifa.sifa_go.data.model.FiscalizadorHeartbeatRequest
import com.sifa.sifa_go.data.model.PlateInfoResponse
import com.sifa.sifa_go.data.model.TipoInfraccionResponse
import retrofit2.http.POST
import retrofit2.http.GET
import com.sifa.sifa_go.data.model.InfraccionResponse
import com.sifa.sifa_go.data.model.InfraccionHistoryItem
import com.sifa.sifa_go.data.model.SpringPageResponse
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path

interface CoreApiService {
    @GET("/core/api/v1/vehiculos/id/{id}")
    suspend fun getPlateInfo(
        @Path("id") id: String
    ): PlateInfoResponse

    @GET("/core/api/v1/tipoInfracciones/id/{id}")
    suspend fun getTipoInfraccion(
        @Path("id") id: String
    ): TipoInfraccionResponse

    @GET("/core/api/v1/tipoInfracciones/all")
    suspend fun getAllTipoInfracciones(): Response<SpringPageResponse<TipoInfraccionResponse>>

    @Multipart
    @POST("/core/api/v1/infracciones")
    suspend fun createInfraccion(
        @Part("infraccion") request: RequestBody,
        @Part fotos: List<MultipartBody.Part>
    ): InfraccionResponse

    @GET("/core/api/v1/infracciones/all")
    suspend fun getInfractionsHistory(
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("user") user: String?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<SpringPageResponse<InfraccionHistoryItem>>

    @POST("/core/api/v1/fis-activity/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: FiscalizadorHeartbeatRequest
    ): Response<Unit>
}

object CoreRetrofitClient {
    val apiService: CoreApiService by lazy {
        NetworkModule.retrofit.create(CoreApiService::class.java)
    }
}