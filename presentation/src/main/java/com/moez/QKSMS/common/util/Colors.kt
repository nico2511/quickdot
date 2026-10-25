/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package dev.octoshrimpy.quik.common.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.content.res.getColorOrThrow
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.util.extensions.getColorCompat
import dev.octoshrimpy.quik.model.Recipient
import dev.octoshrimpy.quik.util.AccentPresets
import dev.octoshrimpy.quik.util.Preferences
import dev.octoshrimpy.quik.util.ProBillingHook
import dev.octoshrimpy.quik.util.formatAccentHex
import dev.octoshrimpy.quik.util.resolveEffectiveAccentStored
import io.reactivex.Observable
import io.reactivex.functions.Function3
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue
import kotlin.math.pow

@Singleton
class Colors @Inject constructor(
    private val context: Context,
    private val prefs: Preferences,
    private val proBillingHook: ProBillingHook
) {

    data class Accent(val nameRes: Int, val color: Int)

    /**
     * Print inks for the monochrome dot-matrix UI. Signal red is the default.
     * Anything saved from the old Material grid is snapped to the nearest one.
     */
    val accents: List<Accent> = listOf(
        Accent(R.string.accent_signal, AccentPresets.SIGNAL),
        Accent(R.string.accent_cobalt, AccentPresets.COBALT),
        Accent(R.string.accent_pine, AccentPresets.PINE),
        Accent(R.string.accent_oxide, AccentPresets.OXIDE),
        Accent(R.string.accent_plum, AccentPresets.PLUM),
        Accent(R.string.accent_graphite, AccentPaint.GRAPHITE),
        Accent(R.string.accent_teal, AccentPresets.TEAL),
        Accent(R.string.accent_dusk, AccentPresets.DUSK),
        Accent(R.string.accent_honey, AccentPresets.HONEY),
        Accent(R.string.accent_fog, AccentPresets.FOG),
        Accent(R.string.accent_lichen, AccentPresets.LICHEN),
        Accent(R.string.accent_rose, AccentPresets.ROSE),
    )

    fun snapAccent(color: Int): Int {
        AccentPaint.canonicalize(color)?.let { return it }
        return AccentPresets.snapTowardPreset(color)
    }

    fun isPresetAccent(color: Int): Boolean = AccentPresets.isPreset(snapAccent(color))

    /** Effective accent for the current Pro state, painted for night mode. */
    fun accentColor(): Int = paintedAccent(prefs.theme().get())

    /**
     * Canonical accent, shifted when Graphite would sit on a night surface.
     * Callers that save a colour keep [snapAccent]. Swatches and labels use this
     * without the Pro gate so the stored choice stays visible in the picker.
     */
    fun displayAccent(color: Int): Int = AccentPaint.display(snapAccent(color), night())

    /**
     * Single resolver for painting: Pro gate, then graphite night shift.
     */
    fun paintedAccent(storedColor: Int, proUnlocked: Boolean = proBillingHook.isUnlocked()): Int {
        val effective = resolveEffectiveAccentStored(
            snapAccent(storedColor),
            proBillingHook.enforcementEnabled,
            proUnlocked
        )
        return AccentPaint.display(effective, night())
    }

    private fun night(): Boolean {
        val system = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        return AccentPaint.night(prefs.nightMode.get(), prefs.night.get(), system)
    }

    fun accentLabel(color: Int = accentColor()): String {
        val snapped = snapAccent(color)
        val preset = accents.find { it.color == snapped }
        return preset?.let { context.getString(it.nameRes) } ?: "#${formatAccentHex(snapped)}"
    }

    fun themeSurfaceColor(): Int = context.getColorCompat(
        if (night()) R.color.backgroundDark else R.color.backgroundLight
    )

    /**
     * Accent for one conversation. On the Play flavor a free account keeps
     * [accentColor] on the conversation it chose and signal red on the rest.
     * Pro, and the FOSS flavor, keep [accentColor] everywhere.
     */
    fun accentForThread(threadId: Long): Int {
        return if (proBillingHook.accentAppliesTo(threadId)) accentColor()
        else paintedAccent(Preferences.DEFAULT_THEME_COLOR)
    }

    /** True when the chosen accent is not limited to one conversation. */
    fun styleIsAppWide(): Boolean {
        return !proBillingHook.enforcementEnabled || proBillingHook.isUnlocked()
    }

    data class Theme(val theme: Int, private val colors: Colors) {
        val highlight by lazy { colors.highlightColorForTheme(theme) }
        val textPrimary by lazy { colors.textPrimaryOnThemeForColor(theme) }
        val textSecondary by lazy { colors.textSecondaryOnThemeForColor(theme) }
        val textTertiary by lazy { colors.textTertiaryOnThemeForColor(theme) }
    }

    val materialColors: List<List<Int>> = listOf(
        R.array.material_red,
        R.array.material_pink,
        R.array.material_purple,
        R.array.material_deep_purple,
        R.array.material_indigo,
        R.array.material_blue,
        R.array.material_light_blue,
        R.array.material_cyan,
        R.array.material_teal,
        R.array.material_green,
        R.array.material_light_green,
        R.array.material_lime,
        R.array.material_yellow,
        R.array.material_amber,
        R.array.material_orange,
        R.array.material_deep_orange,
        R.array.material_brown,
        R.array.material_gray,
        R.array.material_blue_gray)
            .map { res -> context.resources.obtainTypedArray(res) }
            .map { typedArray -> (0 until typedArray.length()).map(typedArray::getColorOrThrow) }

    private val minimumContrastRatio = 2

    // Cache these values so they don't need to be recalculated
    private val primaryTextLuminance = measureLuminance(context.getColorCompat(R.color.textPrimaryDark))
    private val secondaryTextLuminance = measureLuminance(context.getColorCompat(R.color.textSecondaryDark))
    private val tertiaryTextLuminance = measureLuminance(context.getColorCompat(R.color.textTertiaryDark))

    /**
     * Monochrome avatar colour. List rows use this so per-contact colours stay
     * off the conversation list; the red accent is reserved for unread and actions.
     */
    fun neutral(): Theme {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        val color = context.getColorCompat(
            if (night) R.color.avatarNeutralDark else R.color.avatarNeutralLight
        )
        return Theme(color, this)
    }

    /**
     * Stable grey for one address. Five slots, never a hue and never the signal red.
     * Light greys keep white initials at 6:1 or better.
     * Night greys keep #111111 initials at 10:1 or better, and stay above 9:1 on #1B1B1B.
     */
    fun speakerInk(address: String): Int {
        val slots = intArrayOf(
            R.color.speakerInk0,
            R.color.speakerInk1,
            R.color.speakerInk2,
            R.color.speakerInk3,
            R.color.speakerInk4
        )
        val index = if (address.isEmpty()) 0 else address.hashCode().absoluteValue % slots.size
        return context.getColorCompat(slots[index])
    }

    @Suppress("UNUSED_PARAMETER")
    fun theme(recipient: Recipient? = null): Theme {
        return Theme(accentColor(), this)
    }

    @Suppress("UNUSED_PARAMETER")
    fun themeObservable(recipient: Recipient? = null): Observable<Theme> {
        return Observable.combineLatest(
                prefs.theme().asObservable(),
                prefs.quickdotPro.asObservable(),
                prefs.styledThreadId.asObservable(),
                Function3<Int, Boolean, Long, Theme> { color, proUnlocked, _ ->
                    Theme(paintedAccent(color, proUnlocked), this@Colors)
                }
        )
    }

    fun highlightColorForTheme(theme: Int): Int = FloatArray(3)
            .apply { Color.colorToHSV(theme, this) }
            .let { hsv -> hsv.apply { set(2, 0.75f) } } // 75% value
            .let { hsv -> Color.HSVToColor(85, hsv) } // 33% alpha

    fun textPrimaryOnThemeForColor(color: Int): Int = color
            .let { theme -> measureLuminance(theme) }
            .let { themeLuminance -> primaryTextLuminance / themeLuminance }
            .let { contrastRatio -> contrastRatio < minimumContrastRatio }
            .let { contrastRatio -> if (contrastRatio) R.color.textPrimary else R.color.textPrimaryDark }
            .let { res -> context.getColorCompat(res) }

    fun textSecondaryOnThemeForColor(color: Int): Int = color
            .let { theme -> measureLuminance(theme) }
            .let { themeLuminance -> secondaryTextLuminance / themeLuminance }
            .let { contrastRatio -> contrastRatio < minimumContrastRatio }
            .let { contrastRatio -> if (contrastRatio) R.color.textSecondary else R.color.textSecondaryDark }
            .let { res -> context.getColorCompat(res) }

    fun textTertiaryOnThemeForColor(color: Int): Int = color
            .let { theme -> measureLuminance(theme) }
            .let { themeLuminance -> tertiaryTextLuminance / themeLuminance }
            .let { contrastRatio -> contrastRatio < minimumContrastRatio }
            .let { contrastRatio -> if (contrastRatio) R.color.textTertiary else R.color.textTertiaryDark }
            .let { res -> context.getColorCompat(res) }

    /**
     * Measures the luminance value of a color to be able to measure the contrast ratio between two materialColors
     * Based on https://stackoverflow.com/a/9733420
     */
    private fun measureLuminance(color: Int): Double {
        val array = intArrayOf(Color.red(color), Color.green(color), Color.blue(color))
                .map { if (it < 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }

        return 0.2126 * array[0] + 0.7152 * array[1] + 0.0722 * array[2] + 0.05
    }

}
