package dev.octoshrimpy.quik.util

import org.junit.Assert.assertEquals
import org.junit.Test

class EffectiveAccentTest {

    private val signal = Preferences.DEFAULT_THEME_COLOR
    private val cobalt = 0xFF2457C5.toInt()

    @Test
    fun playFreePaintsSignal() {
        assertEquals(
            signal,
            resolveEffectiveAccentStored(cobalt, enforcementEnabled = true, proUnlocked = false)
        )
    }

    @Test
    fun playProPaintsChosenAccent() {
        assertEquals(
            cobalt,
            resolveEffectiveAccentStored(cobalt, enforcementEnabled = true, proUnlocked = true)
        )
    }

    @Test
    fun proLostStillResolvesToSignalWhileChoiceStaysInPrefs() {
        assertEquals(
            signal,
            resolveEffectiveAccentStored(cobalt, enforcementEnabled = true, proUnlocked = false)
        )
    }

    @Test
    fun fossPaintsChosenEvenWhenProFlagIsFalse() {
        assertEquals(
            cobalt,
            resolveEffectiveAccentStored(cobalt, enforcementEnabled = false, proUnlocked = false)
        )
    }
}
