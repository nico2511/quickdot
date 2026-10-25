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
package dev.octoshrimpy.quik.common.util

/**
 * Size of a single-line hint so it stays inside [availablePx].
 *
 * Doto bold is wider than Geist. At 14sp the French inbox hint
 * ("Rechercher dans les conversations…") is about 750px, and the
 * conversation-list pill leaves about 660px for text. Scaling keeps
 * the whole hint visible. The floor stops the hint becoming a caption
 * when the field is very narrow. Typed text keeps [restingPx].
 */
internal fun fittedHintSizePx(
    restingPx: Float,
    hintWidthPx: Float,
    availablePx: Float,
    floorPx: Float
): Float {
    if (restingPx <= 0f || availablePx <= 0f || hintWidthPx <= 0f) return restingPx
    if (hintWidthPx <= availablePx) return restingPx
    val scaled = restingPx * availablePx / hintWidthPx
    val floor = if (floorPx in 0f..restingPx) floorPx else 0f
    return if (floor > 0f && scaled < floor) floor else scaled.coerceAtMost(restingPx)
}
