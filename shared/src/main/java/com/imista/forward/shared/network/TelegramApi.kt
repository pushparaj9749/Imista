package com.imista.forward.shared.network

import com.imista.forward.shared.model.*
import retrofit2.Response
import retrofit2.http.*

interface TelegramApi {

    @GET("bot{token}/getMe")
    suspend fun getMe(@Path("token") token: String): Response<TelegramResponse<TelegramUser>>

    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body request: SendMessageRequest
    ): Response<TelegramResponse<TelegramMessage>>

    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @Query("offset") offset: Long?,
        @Query("limit") limit: Int?,
        @Query("timeout") timeout: Int?,
        @Query("allowed_updates") allowedUpdates: String?
    ): Response<TelegramResponse<List<TelegramUpdate>>>

    @GET("bot{token}/getChat")
    suspend fun getChat(
        @Path("token") token: String,
        @Query("chat_id") chatId: String
    ): Response<TelegramResponse<TelegramChat>>
}
