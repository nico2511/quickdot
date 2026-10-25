package dev.octoshrimpy.quik.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageQuoteGateTest {

    @Test
    fun playFreeCannotStartQuote() {
        assertFalse(canStartMessageQuote(enforcementEnabled = true, proUnlocked = false))
        assertTrue(messageQuoteLocked(enforcementEnabled = true, proUnlocked = false))
    }

    @Test
    fun playProCanStartQuote() {
        assertTrue(canStartMessageQuote(enforcementEnabled = true, proUnlocked = true))
        assertFalse(messageQuoteLocked(enforcementEnabled = true, proUnlocked = true))
    }

    @Test
    fun proLostCannotStartQuote() {
        assertFalse(canStartMessageQuote(enforcementEnabled = true, proUnlocked = false))
    }

    @Test
    fun fossAlwaysCanStartQuote() {
        assertTrue(canStartMessageQuote(enforcementEnabled = false, proUnlocked = false))
        assertFalse(messageQuoteLocked(enforcementEnabled = false, proUnlocked = false))
    }
}
