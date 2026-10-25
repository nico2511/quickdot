package dev.octoshrimpy.quik.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationAccentTest {

    private val signal = Preferences.DEFAULT_THEME_COLOR
    private val cobalt = AccentPresets.COBALT
    private val pine = AccentPresets.PINE

    @Test
    fun conversationOverrideWinsOverGlobalWhenPro() {
        assertEquals(
            pine,
            resolveEffectiveAccentForThread(
                hasConversationAccent = true,
                conversationAccent = pine,
                globalAccent = cobalt,
                enforcementEnabled = true,
                proUnlocked = true,
            )
        )
    }

    @Test
    fun unsetConversationUsesGlobalWhenPro() {
        assertEquals(
            cobalt,
            resolveEffectiveAccentForThread(
                hasConversationAccent = false,
                conversationAccent = pine,
                globalAccent = cobalt,
                enforcementEnabled = true,
                proUnlocked = true,
            )
        )
    }

    @Test
    fun playFreePaintsSignalEvenWithConversationOverride() {
        assertEquals(
            signal,
            resolveEffectiveAccentForThread(
                hasConversationAccent = true,
                conversationAccent = pine,
                globalAccent = cobalt,
                enforcementEnabled = true,
                proUnlocked = false,
            )
        )
    }

    @Test
    fun proLostPaintsSignalWhileStoredOverridesRemain() {
        assertEquals(
            signal,
            resolveEffectiveAccentForThread(
                hasConversationAccent = true,
                conversationAccent = pine,
                globalAccent = cobalt,
                enforcementEnabled = true,
                proUnlocked = false,
            )
        )
    }

    @Test
    fun fossPaintsConversationOverrideWithoutProFlag() {
        assertEquals(
            pine,
            resolveEffectiveAccentForThread(
                hasConversationAccent = true,
                conversationAccent = pine,
                globalAccent = cobalt,
                enforcementEnabled = false,
                proUnlocked = false,
            )
        )
    }

    @Test
    fun resetFallsBackToGlobalStoredAccent() {
        assertEquals(
            cobalt,
            resolveStoredAccentForThread(
                hasConversationAccent = false,
                conversationAccent = pine,
                globalAccent = cobalt,
            )
        )
    }
}
