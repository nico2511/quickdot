package dev.octoshrimpy.quik.billing

import dev.octoshrimpy.quik.util.Preferences
import dev.octoshrimpy.quik.util.resolveEffectiveAccentForThread
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Play accent gating is enforced in [resolveEffectiveAccentForThread], not per-thread ids.
 */
class ProEntitlementTest {

    private val signal = Preferences.DEFAULT_THEME_COLOR
    private val cobalt = 0xFF2457C5.toInt()

    @Test
    fun fossAlwaysPaintsChosenAccent() {
        assertEquals(
            cobalt,
            resolveEffectiveAccentForThread(
                hasConversationAccent = false,
                conversationAccent = signal,
                globalAccent = cobalt,
                enforcementEnabled = false,
                proUnlocked = false,
            )
        )
    }

    @Test
    fun playProPaintsChosenAccent() {
        assertEquals(
            cobalt,
            resolveEffectiveAccentForThread(
                hasConversationAccent = false,
                conversationAccent = signal,
                globalAccent = cobalt,
                enforcementEnabled = true,
                proUnlocked = true,
            )
        )
    }

    @Test
    fun playFreePaintsSignal() {
        assertEquals(
            signal,
            resolveEffectiveAccentForThread(
                hasConversationAccent = true,
                conversationAccent = cobalt,
                globalAccent = cobalt,
                enforcementEnabled = true,
                proUnlocked = false,
            )
        )
    }
}
