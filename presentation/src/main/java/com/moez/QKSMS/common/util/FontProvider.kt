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
import androidx.core.content.res.ResourcesCompat
import dev.octoshrimpy.quik.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bundled OFL faces. Doto is for headings and numbers, Space Mono for labels,
 * Inter for body and message text. Nothing is downloaded at runtime.
 *
 * [displayBold] is the weight avatar initials and the thread title use.
 * Regular Doto's dots vanish at those sizes and the letters read as Inter.
 */
@Singleton
class FontProvider @Inject constructor(context: Context) {

    val body: Typeface = load(context, R.font.inter, Typeface.SANS_SERIF)
    val display: Typeface = load(context, R.font.doto, Typeface.SANS_SERIF)
    val displayBold: Typeface = load(context, R.font.doto_bold, display)
    val label: Typeface = load(context, R.font.space_mono, Typeface.MONOSPACE)

    private fun load(context: Context, fontRes: Int, fallback: Typeface): Typeface {
        return try {
            ResourcesCompat.getFont(context, fontRes) ?: fallback
        } catch (e: Exception) {
            Timber.w(e, "Bundled font failed to load")
            fallback
        }
    }

}
