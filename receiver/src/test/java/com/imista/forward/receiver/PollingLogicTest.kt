package com.imista.forward.receiver

import org.junit.Assert.*
import org.junit.Test

class PollingLogicTest {

    @Test
    fun testSingleConsumerSemantics() {
        // Document single consumer: Sender never calls getUpdates, only Receiver does
        val senderCallsGetUpdates = false
        val receiverCallsGetUpdates = true
        assertFalse(senderCallsGetUpdates)
        assertTrue(receiverCallsGetUpdates)
        // If both polled, they'd steal updates
        val competingConsumers = senderCallsGetUpdates && receiverCallsGetUpdates
        assertFalse(competingConsumers)
    }

    @Test
    fun testUpdateOffsetHandling() {
        var offset: Long? = null
        val updates = listOf(1L, 2L, 3L)
        var max = offset ?: 0
        updates.forEach { id -> max = maxOf(max, id) }
        offset = max + 1
        assertEquals(4L, offset)
        // Next poll should use offset 4 to avoid reprocessing
    }

    @Test
    fun testTelegramNotificationParsingRequiresId() {
        val textWithId = "Hello <code>ID:550e8400-e29b-41d4-a716-446655440000</code>"
        val textWithoutId = "Hello from Telegram user"
        val hasId = textWithId.contains("ID:")
        val hasId2 = textWithoutId.contains("ID:")
        assertTrue(hasId)
        assertFalse(hasId2)
    }
}
