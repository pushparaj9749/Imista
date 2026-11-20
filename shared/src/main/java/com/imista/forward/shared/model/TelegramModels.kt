package com.imista.forward.shared.model

import com.google.gson.annotations.SerializedName

// Telegram Bot API models (subset)

data class TelegramResponse<T>(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("result") val result: T?,
    @SerializedName("description") val description: String?,
    @SerializedName("error_code") val errorCode: Int?,
    @SerializedName("parameters") val parameters: TelegramParameters?
)

data class TelegramParameters(
    @SerializedName("retry_after") val retryAfter: Int?,
    @SerializedName("migrate_to_chat_id") val migrateToChatId: Long?
)

data class TelegramUser(
    @SerializedName("id") val id: Long,
    @SerializedName("is_bot") val isBot: Boolean,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("username") val username: String?
)

data class TelegramChat(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("title") val title: String?
)

data class TelegramMessage(
    @SerializedName("message_id") val messageId: Int,
    @SerializedName("from") val from: TelegramUser?,
    @SerializedName("chat") val chat: TelegramChat,
    @SerializedName("date") val date: Long,
    @SerializedName("text") val text: String?
)

data class TelegramUpdate(
    @SerializedName("update_id") val updateId: Long,
    @SerializedName("message") val message: TelegramMessage?,
    @SerializedName("channel_post") val channelPost: TelegramMessage?,
    @SerializedName("edited_message") val editedMessage: TelegramMessage?
)

data class SendMessageRequest(
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("text") val text: String,
    @SerializedName("parse_mode") val parseMode: String? = "HTML",
    @SerializedName("disable_notification") val disableNotification: Boolean? = false,
    @SerializedName("disable_web_page_preview") val disableWebPagePreview: Boolean = true
)

data class GetUpdatesRequest(
    @SerializedName("offset") val offset: Long? = null,
    @SerializedName("limit") val limit: Int? = 10,
    @SerializedName("timeout") val timeout: Int? = 0,
    @SerializedName("allowed_updates") val allowedUpdates: List<String>? = null
)
