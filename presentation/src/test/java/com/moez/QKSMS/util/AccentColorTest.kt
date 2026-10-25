package dev.octoshrimpy.quik.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {

    @Test
    fun parseAccentHexAcceptsHashAndPlain() {
        assertEquals(0xFFE10600.toInt(), parseAccentHex("#E10600"))
        assertEquals(0xFFE10600.toInt(), parseAccentHex("e10600"))
    }

    @Test
    fun parseAccentHexRejectsInvalid() {
        assertNull(parseAccentHex(""))
        assertNull(parseAccentHex("GGGGGG"))
        assertNull(parseAccentHex("E1060"))
        assertNull(parseAccentHex("#E106001"))
    }

    @Test
    fun formatAccentHexIsUppercaseRgb() {
        assertEquals("E10600", formatAccentHex(0xFFE10600.toInt()))
    }

    @Test
    fun textColorOnAccentPicksHigherContrast() {
        assertEquals(0xFFFFFFFF.toInt(), textColorOnAccent(0xFFE10600.toInt()))
        assertEquals(0xFF000000.toInt(), textColorOnAccent(0xFFB8860B.toInt()))
    }

    @Test
    fun accentContrastTooLowFlagsPaleOnWhite() {
        val surface = 0xFFFFFF.toInt()
        assertTrue(accentContrastTooLow(0xFFF3F3F3.toInt(), surface))
        assertFalse(accentContrastTooLow(0xFFE10600.toInt(), surface))
    }

    @Test
    fun accentContrastRatioIsSymmetric() {
        val ratio = accentContrastRatio(0xFFE10600.toInt(), 0xFFFFFF.toInt())
        assertEquals(ratio, accentContrastRatio(0xFFFFFF.toInt(), 0xFFE10600.toInt()), 0.001)
        assertTrue(ratio > WCAG_ACCENT_SURFACE_MIN_RATIO)
    }
}
