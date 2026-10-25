/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 * Copyright (C) 2026 nico2511
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
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import dev.octoshrimpy.quik.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bundled OFL faces. Doto is for headings and numbers, Space Mono for labels,
 * Geist for body and message text. Nothing is downloaded at runtime.
 *
 * [displayBold] is the weight avatar initials and the thread title use.
 * Regular Doto's dots vanish at those sizes and the letters read as Geist.
 *
 * The four static Doto files used to share a family name, and the bold file carried
 * a variable-font STAT table with no fvar. Android 14's font matcher (Nothing OS)
 * then resolved "bold" back to the regular face or to the system sans. The
 * family below sets each weight explicitly so a later weight adjustment stays
 * inside Doto.
 *
 * Geist ships as a single variable file. On API 29+ the family pins 400/500/700
 * so bold and medium weights stay inside Geist on Android 14.
 */
@Singleton
class FontProvider @Inject constructor(context: Context) {

    val body: Typeface
    val label: Typeface = load(context, R.font.space_mono, Typeface.MONOSPACE)
    val display: Typeface
    val displayBold: Typeface

    init {
        val bodyFallback = load(context, R.font.geist, Typeface.SANS_SERIF)
        body = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            geistFamilyOrNull(context) ?: bodyFallback
        } else {
            bodyFallback
        }

        val regular = load(context, R.font.doto_regular, Typeface.SANS_SERIF)
        val boldFile = load(context, R.font.doto_bold, regular)
        val family = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            dotoFamilyOrNull(context)
        } else {
            null
        }
        display = family?.let { faceAt(it, 400) } ?: regular
        displayBold = family?.let { faceAt(it, 700) } ?: boldFile
    }

    private fun load(context: Context, fontRes: Int, fallback: Typeface): Typeface {
        return try {
            ResourcesCompat.getFont(context, fontRes) ?: fallback
        } catch (e: Exception) {
            Timber.w(e, "Bundled font failed to load")
            fallback
        }
    }

    private fun geistFamilyOrNull(context: Context): Typeface? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val resources = context.resources
            fun font(weight: Int): Font {
                return Font.Builder(resources, R.font.geist_wght).setWeight(weight).build()
            }
            val family = FontFamily.Builder(font(400))
                .addFont(font(500))
                .addFont(font(700))
                .build()
            Typeface.CustomFallbackBuilder(family)
                .setStyle(FontStyle(400, FontStyle.FONT_SLANT_UPRIGHT))
                .build()
        } catch (e: Exception) {
            Timber.w(e, "Geist family failed to build")
            null
        }
    }

    private fun dotoFamilyOrNull(context: Context): Typeface? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val resources = context.resources
            fun font(id: Int, weight: Int): Font {
                return Font.Builder(resources, id).setWeight(weight).build()
            }
            val family = FontFamily.Builder(font(R.font.doto_regular, 400))
                .addFont(font(R.font.doto_medium, 500))
                .addFont(font(R.font.doto_bold, 700))
                .addFont(font(R.font.doto_black, 900))
                .build()
            Typeface.CustomFallbackBuilder(family)
                .setStyle(FontStyle(400, FontStyle.FONT_SLANT_UPRIGHT))
                .build()
        } catch (e: Exception) {
            Timber.w(e, "Doto family failed to build")
            null
        }
    }

    private fun faceAt(family: Typeface, weight: Int): Typeface {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(family, weight, false)
        } else {
            family
        }
    }

}
