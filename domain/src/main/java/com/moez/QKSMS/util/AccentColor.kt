package dev.octoshrimpy.quik.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** WCAG 2.x relative luminance for sRGB colours (0.0–1.0). */
fun accentRelativeLuminance(color: Int): Double {
    fun channel(value: Int): Double {
        val v = value / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    val r = channel((color shr 16) and 0xFF)
    val g = channel((color shr 8) and 0xFF)
    val b = channel(color and 0xFF)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** WCAG contrast ratio between two opaque sRGB colours. */
fun accentContrastRatio(foreground: Int, background: Int): Double {
    val l1 = accentRelativeLuminance(foreground) + 0.05
    val l2 = accentRelativeLuminance(background) + 0.05
    return max(l1, l2) / min(l1, l2)
}

/** Black or white, whichever contrasts more with the accent fill. */
fun textColorOnAccent(accent: Int): Int {
    val rgb = accent and 0xFFFFFF
    val onBlack = accentContrastRatio(0x000000, rgb)
    val onWhite = accentContrastRatio(0xFFFFFF, rgb)
    return if (onBlack >= onWhite) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
}

/**
 * Parses `#RRGGBB` or `RRGGBB` (case insensitive). Returns opaque ARGB or null.
 */
fun parseAccentHex(input: String): Int? {
    val body = input.trim().removePrefix("#")
    if (body.length != 6 || body.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) {
        return null
    }
    return 0xFF000000.toInt() or body.uppercase().toInt(16)
}

fun formatAccentHex(color: Int): String {
    return "%06X".format(color and 0xFFFFFF)
}

/**
 * True when [accent] does not meet [minRatio] against [surface] (WCAG contrast).
 * Surfaces are the light/dark page backgrounds used on the theme screen.
 */
fun accentContrastTooLow(accent: Int, surface: Int, minRatio: Double = WCAG_ACCENT_SURFACE_MIN_RATIO): Boolean {
    return accentContrastRatio(accent and 0xFFFFFF, surface and 0xFFFFFF) < minRatio
}

/** Minimum contrast for accent fills against the page background (WCAG 1.4.11 non-text). */
const val WCAG_ACCENT_SURFACE_MIN_RATIO = 3.0

/** Preset swatches shown on the theme screen (Signal first). */
object AccentPresets {
    const val SIGNAL = 0xFFE10600.toInt()
    const val COBALT = 0xFF2457C5.toInt()
    const val PINE = 0xFF1B7F4E.toInt()
    const val OXIDE = 0xFFC44914.toInt()
    const val PLUM = 0xFF8A3D6E.toInt()
    const val GRAPHITE = 0xFF3A3A3A.toInt()
    const val TEAL = 0xFF0A7A7A.toInt()
    const val DUSK = 0xFF4E5088.toInt()
    const val HONEY = 0xFFB8860B.toInt()
    const val FOG = 0xFF5A7A96.toInt()
    const val LICHEN = 0xFF5F7A42.toInt()
    const val ROSE = 0xFFA63D5C.toInt()

    val colors: List<Int> = listOf(
        SIGNAL, COBALT, PINE, OXIDE, PLUM, GRAPHITE,
        TEAL, DUSK, HONEY, FOG, LICHEN, ROSE
    )

    fun isPreset(color: Int): Boolean = colors.contains(opaqueRgb(color))

    /**
     * Snap legacy Material picks to the nearest preset when every channel is within
     * [LEGACY_SNAP_CHANNEL_DELTA]. Custom hex accents outside that band are kept.
     */
    fun snapTowardPreset(color: Int): Int {
        val rgb = opaqueRgb(color)
        if (colors.contains(rgb)) return rgb
        val nearest = colors.minBy { channelDistanceSq(rgb, it) }
        val maxDelta = maxChannelDelta(rgb, nearest)
        return if (maxDelta <= LEGACY_SNAP_CHANNEL_DELTA) nearest else rgb
    }

    private fun opaqueRgb(color: Int): Int = 0xFF000000.toInt() or (color and 0xFFFFFF)

    private fun channelDistanceSq(a: Int, b: Int): Int {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return dr * dr + dg * dg + db * db
    }

    private fun maxChannelDelta(a: Int, b: Int): Int {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return max(max(dr.absoluteValue, dg.absoluteValue), db.absoluteValue)
    }

    private val Int.absoluteValue: Int get() = if (this < 0) -this else this

    const val LEGACY_SNAP_CHANNEL_DELTA = 28
}

fun isPresetAccentLocked(
    presetColor: Int,
    enforcementEnabled: Boolean,
    proUnlocked: Boolean,
): Boolean {
    if (!enforcementEnabled || proUnlocked) return false
    return opaqueAccentRgb(presetColor) != Preferences.DEFAULT_THEME_COLOR
}

fun isHexAccentLocked(enforcementEnabled: Boolean, proUnlocked: Boolean): Boolean {
    return enforcementEnabled && !proUnlocked
}

private fun opaqueAccentRgb(color: Int): Int = 0xFF000000.toInt() or (color and 0xFFFFFF)
