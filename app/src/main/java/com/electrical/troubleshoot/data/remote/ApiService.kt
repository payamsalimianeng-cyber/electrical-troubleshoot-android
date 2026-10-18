package com.electrical.troubleshoot.data.remote

import com.electrical.troubleshoot.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): ApiResponse<User>

    // Rooms
    @GET("rooms")
    suspend fun getRooms(): ApiResponse<List<Room>>

    @GET("rooms/{id}")
    suspend fun getRoom(@Path("id") id: String): ApiResponse<Room>

    // Mechanisms
    @GET("mechanisms/room/{roomId}")
    suspend fun getMechanismsByRoom(@Path("roomId") roomId: String): ApiResponse<List<Mechanism>>

    @GET("mechanisms/{id}")
    suspend fun getMechanism(@Path("id") id: String): ApiResponse<Mechanism>

    @Multipart
    @POST("mechanisms/{id}/pdf")
    suspend fun uploadPdf(
        @Path("id") id: String,
        @Part pdf: MultipartBody.Part
    ): ApiResponse<Mechanism>

    // Sessions
    @POST("sessions")
    suspend fun startSession(@Body body: StartSessionRequest): ApiResponse<Session>

    @GET("sessions/{id}")
    suspend fun getSession(@Path("id") id: String): ApiResponse<Session>

    @PATCH("sessions/{id}/steps/{stepId}")
    suspend fun updateStep(
        @Path("id") sessionId: String,
        @Path("stepId") stepId: String,
        @Body body: UpdateStepRequest
    ): ApiResponse<Session>

    @PATCH("sessions/{id}/finish")
    suspend fun finishSession(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): ApiResponse<Session>

    @POST("sessions/{id}/experience")
    suspend fun createExperience(
        @Path("id") id: String,
        @Body body: Map<String, Any>
    ): ApiResponse<Experience>

    // Experiences
    @GET("experiences")
    suspend fun getExperiences(
        @Query("room") room: String? = null,
        @Query("mechanism") mechanism: String? = null,
        @Query("search") search: String? = null
    ): ApiResponse<List<Experience>>

    @GET("experiences/sops")
    suspend fun getSops(
        @Query("room") room: String? = null,
        @Query("mechanism") mechanism: String? = null
    ): ApiResponse<List<Sop>>

    // PDF / EPLAN extraction
    @Multipart
    @POST("pdf/analyze")
    suspend fun analyzePdf(
        @Part pdf: MultipartBody.Part
    ): EplanAnalyzeResponse

    @POST("pdf/create-from-extract")
    suspend fun createFromExtract(
        @Body body: CreateFromExtractRequest
    ): ApiResponse<CreateFromExtractResult>
}

// Response models for EPLAN extraction
data class EplanExtracted(
    val installationLocation: String? = null,
    val pageDescription: String? = null,
    val projectName: String? = null,
    val drawingNumber: String? = null,
    val cabinet: String? = null,
    val pageNumber: String? = null,
    val suggestedRoomName: String? = null,
    val suggestedRoomCode: String? = null,
    val suggestedMechanismName: String? = null,
    val confidence: Int = 0,
    val rawPreview: String? = null
)

data class EplanAnalyzeResponse(
    val success: Boolean,
    val message: String? = null,
    val hasText: Boolean = false,
    val numPages: Int? = null,
    val extracted: EplanExtracted? = null,
    val tempFile: String? = null
)

data class CreateFromExtractRequest(
    val tempFile: String? = null,
    val roomName: String,
    val roomCode: String? = null,
    val mechanismName: String,
    val mechanismCode: String? = null,
    val location: String? = null
)

data class CreateFromExtractResult(
    val room: Room,
    val mechanism: Mechanism
)
