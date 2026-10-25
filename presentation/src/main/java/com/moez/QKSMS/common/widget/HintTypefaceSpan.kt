/*
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
package dev.octoshrimpy.quik.common.widget

import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.widget.TextView

/**
 * Puts one exact [Typeface] on the hint paint.
 *
 * [TextView.setTypeface](android.widget.TextView) on Android 14 runs the face
 * through [Typeface.create] when the configuration has a font-weight adjustment
 * (Nothing OS, and the system bold-text setting). That lookup misses a bundled
 * face and returns the system sans, so a pinned Doto bold still draws as Geist.
 * A metric span calls [TextPaint.setTypeface] with the instance itself.
 * [Typeface.create] is not used here: it is the call that drops the face.
 */
class HintTypefaceSpan(val typeface: Typeface) : MetricAffectingSpan() {

    override fun updateDrawState(paint: TextPaint) {
        apply(paint)
    }

    override fun updateMeasureState(paint: TextPaint) {
        apply(paint)
    }

    private fun apply(paint: TextPaint) {
        paint.typeface = typeface
        paint.isFakeBoldText = false
    }
}

/**
 * Applies a pinned [typeface] on [view] without [TextView.setTypeface] re-running
 * [Typeface.create] when [Configuration.fontWeightAdjustment] is non-zero.
 */
internal fun applyPinnedTypeface(
    view: TextView,
    typeface: Typeface,
    setTypeface: (Typeface) -> Unit,
) {
    val adjustment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        view.resources.configuration.fontWeightAdjustment
    } else {
        0
    }
    if (adjustment != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val weight = typeface.weight.coerceIn(1, 1000)
        val italic = typeface.isItalic
        // TextView adds fontWeightAdjustment inside setTypeface; pre-compensate so
        // the resolved weight matches the pinned face.
        val compensated = (weight - adjustment).coerceIn(1, 1000)
        setTypeface(Typeface.create(typeface, compensated, italic))
    } else {
        setTypeface(typeface)
    }
}
