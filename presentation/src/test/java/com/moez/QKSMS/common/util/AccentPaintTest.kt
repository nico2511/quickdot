package dev.octoshrimpy.quik.common.util

import dev.octoshrimpy.quik.util.Preferences
import org.junit.Assert.assertEquals
import org.junit.Test

class AccentPaintTest {

    private val cobalt = 0xFF2457C5.toInt()
    private val signal = 0xFFE10600.toInt()

    @Test
    fun graphiteStaysDarkInLight() {
        assertEquals(AccentPaint.GRAPHITE, AccentPaint.display(AccentPaint.GRAPHITE, night = false))
    }

    @Test
    fun graphiteLiftsOnNight() {
        assertEquals(
            AccentPaint.GRAPHITE_ON_NIGHT,
            AccentPaint.display(AccentPaint.GRAPHITE, night = true)
        )
    }

    @Test
    fun nightGraphiteCanonicalizesBackToGraphite() {
        assertEquals(AccentPaint.GRAPHITE, AccentPaint.canonicalize(AccentPaint.GRAPHITE_ON_NIGHT))
        assertEquals(null, AccentPaint.canonicalize(cobalt))
    }

    @Test
    fun otherAccentsDoNotChange() {
        assertEquals(cobalt, AccentPaint.display(cobalt, night = true))
        assertEquals(signal, AccentPaint.display(signal, night = false))
    }

    @Test
    fun nightFollowsTheMode() {
        assertEquals(true, AccentPaint.night(Preferences.NIGHT_MODE_ON, scheduledNight = false, systemNight = false))
        assertEquals(false, AccentPaint.night(Preferences.NIGHT_MODE_OFF, scheduledNight = true, systemNight = true))
        assertEquals(true, AccentPaint.night(Preferences.NIGHT_MODE_AUTO, scheduledNight = true, systemNight = false))
        assertEquals(false, AccentPaint.night(Preferences.NIGHT_MODE_AUTO, scheduledNight = false, systemNight = true))
        assertEquals(true, AccentPaint.night(Preferences.NIGHT_MODE_SYSTEM, scheduledNight = false, systemNight = true))
        assertEquals(false, AccentPaint.night(Preferences.NIGHT_MODE_SYSTEM, scheduledNight = true, systemNight = false))
    }
}
