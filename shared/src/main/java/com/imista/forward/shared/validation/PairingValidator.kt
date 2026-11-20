package com.imista.forward.shared.validation

import com.imista.forward.shared.model.PairingInfo
import java.util.UUID

object PairingValidator {

    fun generatePairingId(): String = UUID.randomUUID().toString()

    fun isValidPairingId(id: String): Boolean {
        return try {
            UUID.fromString(id)
            true
        } catch (e: Exception) { false }
    }

    /** Relay-issued short pairing codes are exactly 6 digits. */
    fun isValidPairingCode(code: String): Boolean = code.matches(Regex("^\\d{6}$"))

    /** Accepts either a legacy UUID pairing id or a new 6-digit relay code. */
    fun isAcceptablePairingId(id: String): Boolean =
        isValidPairingId(id) || isValidPairingCode(id)

    fun isValidBotToken(token: String): Boolean {
        // Telegram bot token format: 123456789:ABC-DEF...
        return token.matches(Regex("^\\d+:[A-Za-z0-9_\\-]+$")) && token.length in 20..60
    }

    fun isValidChatId(chatId: String): Boolean {
        return chatId.matches(Regex("^-?\\d+$")) && chatId.length in 5..20
    }

    fun createPairingQrCodePayload(pairingId: String, chatId: String, role: String): String {
        // QR contains pairingId:chatId:role with base64 for safety
        val raw = "$pairingId|$chatId|$role"
        return android.util.Base64.encodeToString(raw.toByteArray(), android.util.Base64.NO_WRAP)
    }

    fun parseQrPayload(qr: String): PairingInfo? {
        return try {
            val decoded = String(android.util.Base64.decode(qr, android.util.Base64.NO_WRAP))
            val parts = decoded.split("|")
            if (parts.size < 3) return null
            PairingInfo(
                pairingId = parts[0],
                telegramChatId = parts[1],
                deviceRole = if (parts[2] == "SENDER") com.imista.forward.shared.model.DeviceRole.SENDER else com.imista.forward.shared.model.DeviceRole.RECEIVER,
                deviceName = "Paired Device",
                deviceId = parts[0]
            )
        } catch (e: Exception) { null }
    }
}
