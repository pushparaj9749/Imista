package com.imista.forward.shared

import com.imista.forward.shared.model.NotificationPayload
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NotificationPayloadTest {

    @Test
    fun testUniqueKey() {
        val p = NotificationPayload(UUID.randomUUID().toString(), "com.whatsapp", "WhatsApp", "Title", "Text", null, null, null, 1000L)
        assertTrue(p.uniqueKey().contains("com.whatsapp"))
    }

    @Test
    fun testIsDuplicateOf_WithinWindow() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()
        val p1 = NotificationPayload(id1, "com.whatsapp", "WhatsApp", "John", "Hi", null, null, null, ts)
        val p2 = NotificationPayload(id2, "com.whatsapp", "WhatsApp", "John", "Hi", null, null, null, ts + 3000)
        assertTrue(p1.isDuplicateOf(p2, 5000))
    }

    @Test
    fun testIsDuplicateOf_DifferentPackageNotDuplicate() {
        val ts = System.currentTimeMillis()
        val p1 = NotificationPayload(UUID.randomUUID().toString(), "com.whatsapp", "WhatsApp", "Hi", "Text", null, null, null, ts)
        val p2 = NotificationPayload(UUID.randomUUID().toString(), "com.telegram", "Telegram", "Hi", "Text", null, null, null, ts)
        assertFalse(p1.isDuplicateOf(p2, 5000))
    }

    @Test
    fun testDisplayTextPrefersBigText() {
        val p = NotificationPayload(UUID.randomUUID().toString(), "pkg", "App", "T", "short", "long big text", null, null, 0L)
        assertEquals("long big text", p.displayText())
        val p2 = NotificationPayload(UUID.randomUUID().toString(), "pkg", "App", "T", "short", null, null, null, 0L)
        assertEquals("short", p2.displayText())
    }
}
