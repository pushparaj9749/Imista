package com.imista.forward.shared.network

import com.imista.forward.shared.model.NotificationPayload
import retrofit2.Response
import retrofit2.http.*

data class RegisterRequest(
    val pairingId: String,
    val deviceId: String,
    val deviceName: String,
    val role: String // SENDER or RECEIVER
)

data class RegisterResponse(
    val ok: Boolean,
    val pairing: PairingResult?,
    val error: String?
)

data class PairingResult(
    val pairingId: String?,
    val status: String?
)

data class ForwardRequest(
    val pairingId: String,
    val deviceId: String,
    val payload: NotificationPayload
)

data class ForwardResponse(
    val ok: Boolean,
    val stored: Boolean?,
    val telegram: TelegramForwardInfo?,
    val error: String?
)

data class TelegramForwardInfo(
    val ok: Boolean,
    val error: String?
)

data class PollResponse(
    val ok: Boolean,
    val messages: List<RelayMessage>?,
    val count: Int?,
    val error: String?
)

data class RelayMessage(
    val id: String,
    val pairing_id: String?,
    val package_name: String?,
    val app_name: String?,
    val title: String?,
    val text_content: String?,
    val big_text: String?,
    val timestamp: Long?,
    val created_at: Long?,
    val raw: NotificationPayload?
)

interface RelayApi {
    @POST("api/register")
    suspend fun register(@Body req: RegisterRequest): Response<RegisterResponse>

    @GET("api/register")
    suspend fun getPairing(@Query("pairingId") pairingId: String): Response<RegisterResponse>

    @POST("api/forward")
    suspend fun forward(
        @Header("X-Pairing-Id") pairingId: String,
        @Header("X-Device-Id") deviceId: String,
        @Body payload: ForwardRequest
    ): Response<ForwardResponse>

    @GET("api/poll")
    suspend fun poll(
        @Query("pairingId") pairingId: String,
        @Query("deviceId") deviceId: String,
        @Query("since") since: Long,
        @Query("limit") limit: Int
    ): Response<PollResponse>

    @GET("api/health")
    suspend fun health(): Response<Map<String, Any>>
}
