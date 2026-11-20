package com.imista.forward.shared.network

import com.imista.forward.shared.model.NotificationPayload
import retrofit2.Response
import retrofit2.http.*

data class RegisterRequest(
    val pairingId: String? = null,
    val code: String? = null,
    val deviceId: String,
    val deviceName: String,
    val role: String // SENDER or RECEIVER
)

data class RegisterResponse(
    val ok: Boolean,
    val pairing: PairingResult?,
    val error: String?
)

data class LinkResponse(
    val ok: Boolean,
    val linked: Boolean?,
    val chatId: String?,
    val reason: String?,
    val error: String?
)

data class StatusInfo(
    val batteryPct: Int? = null,
    val charging: Boolean? = null,
    val network: String? = null,
    val screenOn: Boolean? = null,
    val screenLocked: Boolean? = null,
    val batterySaver: Boolean? = null,
    val forwarding: Boolean = false,
    val notificationAccess: Boolean = false,
    val appVersion: String? = null,
    val deviceModel: String? = null,
    val storageFreeMb: Int? = null,
    val uptimeMinutes: Int? = null,
    val forwardedTotal: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DeviceStatusRequest(
    val pairingId: String,
    val deviceId: String,
    val status: StatusInfo
)

data class StatusResponse(
    val ok: Boolean,
    val status: StatusInfo? = null,
    val updatedAt: Long? = null,
    val error: String? = null
)

data class RelayBotInfo(
    val ok: Boolean,
    val telegramConfigured: Boolean?,
    val username: String?
)

data class PairingResult(
    val pairingId: String? = null,
    val status: String? = null,
    val code: String? = null,
    val role: String? = null,
    val deviceName: String? = null,
    val telegramLinked: Boolean? = null,
    val botUsername: String? = null,
    val linkUrl: String? = null,
    val senderJoined: Boolean? = null,
    val receiverJoined: Boolean? = null
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
    suspend fun getPairing(@Query("code") code: String): Response<RegisterResponse>

    /** Asks the relay to auto-link the Telegram chat after the user pressed /start &lt;code&gt;. */
    @GET("api/link")
    suspend fun link(@Query("code") code: String): Response<LinkResponse>

    @GET("api/bot")
    suspend fun bot(): Response<RelayBotInfo>

    @POST("api/status")
    suspend fun pushStatus(@Body req: DeviceStatusRequest): Response<StatusResponse>

    @GET("api/status")
    suspend fun getStatus(
        @Query("pairingId") pairingId: String,
        @Query("deviceId") deviceId: String
    ): Response<StatusResponse>

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
