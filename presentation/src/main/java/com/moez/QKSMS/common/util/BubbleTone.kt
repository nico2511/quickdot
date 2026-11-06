/*
 * Tint helpers for conversation bubbles when the user picks a non-default accent (Pro).
 * Keeps fills muted and text at least WCAG AA (4.5:1) against the bubble background.
 */
package dev.octoshrimpy.quik.common.util

import android.graphics.Color
import dev.octoshrimpy.quik.util.accentContrastRatio
import kotlin.math.max
import kotlin.math.min

object BubbleTone {

    private const val MIN_BODY_CONTRAST = 4.5

    /**
     * Outgoing bubble: blend page-tinted base toward accent; dark/muted, not a flat accent swatch.
     */
    fun outgoingFill(accent: Int, night: Boolean): Int {
        val base = if (night) 0xFF1B1B1B.toInt() else 0xFFF3F3F3.toInt()
        return blendTowardReadableBody(base, accent)
    }

    /** Incoming bubble body when using a custom accent: neutral fill (readable with theme primary). */
    fun incomingFill(night: Boolean): Int =
        if (night) 0xFF2A2A2A.toInt() else 0xFFF3F3F3.toInt()

    /** Body text on an outgoing bubble fill, at least [MIN_BODY_CONTRAST] when possible. */
    fun bodyTextForOutgoingFill(fill: Int): Int {
        val candidates = listOf(
            0xFF000000.toInt(),
            0xFF111111.toInt(),
            0xFFFFFFFF.toInt(),
            0xFFE8E8E8.toInt(),
        )
        return candidates.maxBy { accentContrastRatio(it, fill) }
    }

    private fun blendTowardReadableBody(base: Int, accent: Int): Int {
        var t = 0.14f
        var mixed = blendRgb(base, accent, t)
        repeat(16) {
            val text = bodyTextForOutgoingFill(mixed)
            if (accentContrastRatio(text, mixed) >= MIN_BODY_CONTRAST) {
                return saturateIfFlat(mixed, accent)
            }
            t = min(1f, t + 0.055f)
            mixed = blendRgb(base, accent, t)
        }
        return saturateIfFlat(mixed, accent)
    }

    private fun saturateIfFlat(fill: Int, accent: Int): Int {
        val hsvFill = FloatArray(3)
        Color.colorToHSV(fill, hsvFill)
        if (hsvFill[1] >= 0.04f) return fill
        val hsvAccent = FloatArray(3)
        Color.colorToHSV(accent, hsvAccent)
        val out = floatArrayOf(
            hsvAccent[0],
            max(0.06f, min(0.22f, hsvAccent[1] * 0.38f + hsvFill[1])),
            hsvFill[2],
        )
        return Color.HSVToColor(out)
    }

    private fun blendRgb(a: Int, b: Int, t: Float): Int {
        fun ch(ca: Int, cb: Int): Int =
            (ca + (cb - ca) * t).toInt().coerceIn(0, 255)
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        return 0xFF000000.toInt() or
            (ch(ar, br) shl 16) or
            (ch(ag, bg) shl 8) or
            ch(ab, bb)
    }
}
