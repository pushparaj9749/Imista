package com.imista.forward.sender

import org.junit.Assert.*
import org.junit.Test

class RetryLogicTest {

    @Test
    fun testExponentialBackoffDelays() {
        // Simulate WorkManager exponential backoff: 10s * 2^(attempt)
        fun delayFor(attempt: Int): Long {
            val base = 10L
            return base * (1L shl attempt) // 10,20,40,80...
        }
        assertEquals(10L, delayFor(0))
        assertEquals(20L, delayFor(1))
        assertEquals(40L, delayFor(2))
        assertEquals(80L, delayFor(3))
    }

    @Test
    fun testMaxRetryAttempts() {
        val max = com.imista.forward.shared.util.Constants.MAX_RETRY_ATTEMPTS
        assertEquals(5, max)
        // After max attempts, should be marked failed
        var attempts = 0
        var status = "PENDING"
        repeat(max + 1) {
            attempts++
            if (attempts >= max) status = "FAILED"
        }
        assertEquals("FAILED", status)
        assertEquals(6, attempts)
    }

    @Test
    fun testRateLimitHandling() {
        // Simulate parsing retry_after
        val errorBody = """{"ok":false,"error_code":429,"description":"Too Many Requests: retry after 45","parameters":{"retry_after":45}}"""
        val regex = Regex("\"retry_after\"\\s*:\\s*(\\d+)")
        val match = regex.find(errorBody)
        val retryAfter = match?.groupValues?.get(1)?.toInt()
        assertEquals(45, retryAfter)
    }

    @Test
    fun testDuplicateSuppressionWindow() {
        val window = com.imista.forward.shared.util.Constants.DUPLICATE_WINDOW_MS
        assertEquals(7000L, window)
    }
}
