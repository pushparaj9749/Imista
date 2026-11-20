package com.imista.forward.shared.network

import com.imista.forward.shared.model.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Handles Telegram Bot API communication with retry, rate-limit, and error handling.
 * No secrets are logged. Notification content is never logged.
 */
class TelegramRepository(
    private val baseUrl: String = "https://api.telegram.org/"
) {
    private val api: TelegramApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE // never log bodies containing notification content
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TelegramApi::class.java)
    }

    suspend fun verifyBotToken(token: String): Result<TelegramUser> {
        if (token.isBlank()) return Result.failure(IllegalArgumentException("Bot token is empty"))
        return try {
            val resp = api.getMe(token)
            if (!resp.isSuccessful) {
                return Result.failure(Exception("HTTP ${resp.code()}: ${resp.message()}"))
            }
            val body = resp.body()
            if (body?.ok == true && body.result != null) {
                Result.success(body.result)
            } else {
                Result.failure(Exception(body?.description ?: "Invalid token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyChat(token: String, chatId: String): Result<TelegramChat> {
        return try {
            val resp = api.getChat(token, chatId)
            if (!resp.isSuccessful) return Result.failure(Exception("HTTP ${resp.code()}"))
            val body = resp.body()
            if (body?.ok == true && body.result != null) Result.success(body.result)
            else Result.failure(Exception(body?.description ?: "Chat verification failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendNotification(
        token: String,
        chatId: String,
        payload: NotificationPayload,
        encrypt: Boolean = false
    ): TelegramDeliveryResult {
        val formatted = formatPayload(payload, encrypt)
        val req = SendMessageRequest(chatId = chatId, text = formatted, parseMode = "HTML")
        return try {
            val resp = api.sendMessage(token, req)
            if (!resp.isSuccessful) {
                val errorBody = resp.errorBody()?.string()
                val retryAfter = parseRetryAfter(errorBody)
                return TelegramDeliveryResult(false, null, "HTTP ${resp.code()}: $errorBody", retryAfter)
            }
            val body = resp.body()
            if (body?.ok == true) {
                TelegramDeliveryResult(true, body.result?.messageId, null)
            } else {
                val retryAfter = body?.parameters?.retryAfter
                TelegramDeliveryResult(false, null, body?.description ?: "Unknown error", retryAfter)
            }
        } catch (e: Exception) {
            TelegramDeliveryResult(false, null, e.message ?: "Network error")
        }
    }

    suspend fun getUpdates(
        token: String,
        offset: Long?,
        limit: Int = 10,
        timeout: Int = 0
    ): Result<List<TelegramUpdate>> {
        return try {
            val resp = api.getUpdates(token, offset, limit, timeout, null)
            if (!resp.isSuccessful) return Result.failure(Exception("HTTP ${resp.code()}"))
            val body = resp.body()
            if (body?.ok == true) Result.success(body.result ?: emptyList())
            else Result.failure(Exception(body?.description ?: "getUpdates failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatPayload(payload: NotificationPayload, encrypt: Boolean): String {
        // Structured format that receiver can parse reliably
        // Use HTML for Telegram rendering
        val title = escapeHtml(payload.title ?: "")
        val text = escapeHtml(payload.displayText() ?: "")
        val app = escapeHtml(payload.appName)
        val pkg = escapeHtml(payload.packageName)
        val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date(payload.timestamp))

        // If encryption requested, we encrypt title+text body separately and embed as ENC: prefix
        // For now, we format plain; encryption layer can wrap entire message if enabled globally
        // Keep ID hidden but traceable for dedup
        return buildString {
            append("<b>📱 ${app}</b>\n")
            if (title.isNotBlank()) append("<b>${title}</b>\n")
            if (text.isNotBlank()) {
                val truncated = if (text.length > 900) text.take(900) + "…" else text
                append("${truncated}\n")
            }
            append("\n<i>${pkg} • ${time}</i>\n")
            append("<code>ID:${payload.id}</code>")
        }
    }

    fun parsePayloadFromTelegramText(text: String): NotificationPayload? {
        // Attempt to parse formatted message back to payload
        // This is used by receiver's TelegramNotificationListener which reads Telegram app notifications
        // Example text mirrors formatPayload
        return try {
            if (!text.contains("ID:")) return null
            val idRegex = Regex("ID:([a-f0-9\\-]+)", RegexOption.IGNORE_CASE)
            val idMatch = idRegex.find(text) ?: return null
            val id = idMatch.groupValues[1]
            // Heuristic parsing: first line is app, second is title etc.
            // For robustness, we store minimal data; full parsing best-effort
            val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val appLine = lines.firstOrNull() ?: "Unknown"
            val appName = appLine.replace(Regex("<[^>]*>"), "").replace("📱", "").trim()
            // Title is second line if present and not package line
            val title = lines.getOrNull(1)?.replace(Regex("<[^>]*>"), "")?.trim()
            val content = lines.getOrNull(2)?.replace(Regex("<[^>]*>"), "")?.trim()

            NotificationPayload(
                id = id,
                packageName = "unknown", // will be refined if package line parsed
                appName = appName.ifBlank { "Telegram" },
                title = title,
                text = content,
                bigText = content,
                subText = null,
                category = null,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRetryAfter(errorBody: String?): Int? {
        if (errorBody == null) return null
        return try {
            val regex = Regex("\"retry_after\"\\s*:\\s*(\\d+)")
            regex.find(errorBody)?.groupValues?.get(1)?.toInt()
        } catch (e: Exception) { null }
    }

    private fun escapeHtml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
