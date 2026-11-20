package com.imista.forward.shared

import com.imista.forward.shared.model.NotificationPayload
import com.imista.forward.shared.network.TelegramRepository
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class TelegramRepositoryTest {

    @Test
    fun testFormatPayload_containsRequiredFields() {
        val repo = TelegramRepository()
        val p = NotificationPayload(
            id = UUID.randomUUID().toString(),
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            title = "John Doe",
            text = "Hello world",
            bigText = "Hello world verbose",
            subText = null,
            category = "msg",
            timestamp = 1700000000000L
        )
        val formatted = repo.formatPayload(p, false)
        assertTrue(formatted.contains("WhatsApp"))
        assertTrue(formatted.contains("John Doe"))
        assertTrue(formatted.contains("com.whatsapp"))
        assertTrue(formatted.contains(p.id))
    }

    @Test
    fun testParsePayload_ExtractsId() {
        val repo = TelegramRepository()
        val id = UUID.randomUUID().toString()
        val text = "<b>📱 WhatsApp</b>\n<b>John</b>\nHello\n\n<i>com.whatsapp • 2026-11-20 10:00:00</i>\n<code>ID:${id}</code>"
        val parsed = repo.parsePayloadFromTelegramText(text)
        assertNotNull(parsed)
        assertEquals(id, parsed!!.id)
    }

    @Test
    fun testParsePayload_NullWhenNoId() {
        val repo = TelegramRepository()
        val parsed = repo.parsePayloadFromTelegramText("Just a normal telegram message without ID")
        assertNull(parsed)
    }

    @Test
    fun testFormatPayload_TruncatesLongText() {
        val repo = TelegramRepository()
        val longText = "a".repeat(2000)
        val p = NotificationPayload(UUID.randomUUID().toString(), "com.test", "Test", "Title", longText, longText, null, null, System.currentTimeMillis())
        val formatted = repo.formatPayload(p, false)
        // Should be truncated to ~900 chars, so total length limited
        assertTrue(formatted.length < 2000)
    }

    @Test
    fun testEscapeHtml() {
        val repo = TelegramRepository()
        val p = NotificationPayload(UUID.randomUUID().toString(), "com.test", "Test", "<script>", "A & B > C", null, null, null, System.currentTimeMillis())
        val formatted = repo.formatPayload(p, false)
        assertTrue(formatted.contains("&lt;script&gt;"))
        assertTrue(formatted.contains("A &amp; B"))
    }
}
