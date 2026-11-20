package com.imista.forward.shared

import com.imista.forward.shared.validation.PairingValidator
import org.junit.Assert.*
import org.junit.Test

class PairingValidatorTest {

    @Test
    fun testBotToken_Valid() {
        assertTrue(PairingValidator.isValidBotToken("123456789:AAH_fake_token_1234567890abcDEF"))
        assertFalse(PairingValidator.isValidBotToken("invalid"))
        assertFalse(PairingValidator.isValidBotToken(""))
    }

    @Test
    fun testChatId_Valid() {
        assertTrue(PairingValidator.isValidChatId("123456789"))
        assertTrue(PairingValidator.isValidChatId("-100123456789"))
        assertFalse(PairingValidator.isValidChatId("abc"))
    }

    @Test
    fun testPairingQrRoundTrip() {
        val pid = PairingValidator.generatePairingId()
        val chat = "123456789"
        val qr = PairingValidator.createPairingQrCodePayload(pid, chat, "SENDER")
        assertNotNull(qr)
        val parsed = PairingValidator.parseQrPayload(qr)
        assertNotNull(parsed)
        assertEquals(pid, parsed!!.pairingId)
        assertEquals(chat, parsed.telegramChatId)
    }

    @Test
    fun testInvalidPairingId() {
        assertFalse(PairingValidator.isValidPairingId("not-a-uuid"))
        assertTrue(PairingValidator.isValidPairingId("550e8400-e29b-41d4-a716-446655440000"))
    }

    @Test
    fun testGeneratePairingIdIsUnique() {
        val a = PairingValidator.generatePairingId()
        val b = PairingValidator.generatePairingId()
        assertNotEquals(a, b)
    }
}
