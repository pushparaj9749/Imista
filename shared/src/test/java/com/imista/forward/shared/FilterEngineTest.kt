package com.imista.forward.shared

import com.imista.forward.shared.model.AppFilter
import com.imista.forward.shared.model.ForwardingMode
import com.imista.forward.shared.model.NotificationPayload
import com.imista.forward.shared.util.FilterEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FilterEngineTest {

    private fun payload(pkg: String, title: String = "Title", text: String = "Text", ts: Long = System.currentTimeMillis()) =
        NotificationPayload(id = UUID.randomUUID().toString(), packageName = pkg, appName = pkg, title = title, text = text, bigText = text, subText = null, category = null, timestamp = ts)

    @Test
    fun testAllApps_AllowsWhenNotExcluded() {
        val p = payload("com.whatsapp")
        assertTrue(FilterEngine.shouldForward(p, ForwardingMode.ALL_APPS, emptyList()))
    }

    @Test
    fun testAllApps_BlocksExcluded() {
        val p = payload("com.whatsapp")
        val filters = listOf(AppFilter("com.whatsapp", "WhatsApp", isExcluded = true))
        assertFalse(FilterEngine.shouldForward(p, ForwardingMode.ALL_APPS, filters))
    }

    @Test
    fun testSelectedOnly_AllowsOnlySelected() {
        val p1 = payload("com.whatsapp")
        val p2 = payload("com.instagram.android")
        val filters = listOf(AppFilter("com.whatsapp", "WhatsApp", isEnabled = true))
        assertTrue(FilterEngine.shouldForward(p1, ForwardingMode.SELECTED_ONLY, filters))
        assertFalse(FilterEngine.shouldForward(p2, ForwardingMode.SELECTED_ONLY, filters))
    }

    @Test
    fun testExcludeList() {
        val p = payload("com.whatsapp")
        val filters = listOf(AppFilter("com.whatsapp", "WhatsApp", isExcluded = true))
        assertFalse(FilterEngine.shouldForward(p, ForwardingMode.EXCLUDE_LIST, filters))
        val p2 = payload("com.gmail")
        assertTrue(FilterEngine.shouldForward(p2, ForwardingMode.EXCLUDE_LIST, filters))
    }

    @Test
    fun testDuplicateSuppression() {
        val ts = System.currentTimeMillis()
        val p1 = payload("com.whatsapp", "John", "Hi", ts)
        val p2 = payload("com.whatsapp", "John", "Hi", ts + 2000) // within window
        assertTrue(FilterEngine.isDuplicate(p2, listOf(p1), windowMs = 7000))
        val p3 = payload("com.whatsapp", "John", "Hi", ts + 10000)
        assertFalse(FilterEngine.isDuplicate(p3, listOf(p1), windowMs = 7000))
    }

    @Test
    fun testRedaction() {
        val p = payload("com.whatsapp", "Title", "Secret message")
        val redacted = FilterEngine.redactIfNeeded(p, true)
        assertEquals("[Hidden]", redacted.text)
        assertTrue(redacted.isSensitiveRedacted)
    }

    @Test
    fun testDuplicateCacheKey() {
        val p1 = payload("com.test", "A", "B", 10000)
        val p2 = payload("com.test", "A", "B", 10000)
        // should be duplicate via history check
        assertTrue(p1.isDuplicateOf(p2, 5000))
    }
}
