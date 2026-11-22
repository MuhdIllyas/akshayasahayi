package com.akshayasahayi.companion.api

import com.akshayasahayi.companion.models.GenericResponse
import com.akshayasahayi.companion.models.HeartbeatRequest
import com.akshayasahayi.companion.models.PairRequest
import com.akshayasahayi.companion.models.PairResponse
import com.akshayasahayi.companion.models.StatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    // ❌ PUBLIC: Initial pairing
    @POST("api/companion/pair")
    suspend fun pairDevice(@Body request: PairRequest): Response<PairResponse>

    @POST("api/companion/unpair")
    suspend fun unpairDevice(@Header("Authorization") token: String): retrofit2.Response<Any>

    // ✅ PROTECTED: Send battery and network status
    @POST("api/companion/heartbeat")
    suspend fun sendHeartbeat(
        @Header("Authorization") authHeader: String,
        @Body request: HeartbeatRequest
    ): Response<GenericResponse>

    // ✅ PROTECTED: Fetch CRM connection status
    @GET("api/companion/status")
    suspend fun getStatus(
        @Header("Authorization") authHeader: String
    ): Response<StatusResponse>
}