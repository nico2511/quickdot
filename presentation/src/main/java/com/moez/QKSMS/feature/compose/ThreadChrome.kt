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
package dev.octoshrimpy.quik.feature.compose

/**
 * Thread marks still open for a choice. Flip one constant and rebuild.
 * Checked-in values are variant A for the date rule and variant A for the rail.
 *
 * Dots can mark the thread, and they stay small, short, and low-contrast so the
 * messages keep the space and the attention. The date itself stays at reading size.
 *
 * [DATE_RULE_FINE] true draws three tiny dots on each side of the date.
 * false leaves the date centered, with spacing and no dots.
 *
 * [SPROCKET_SUBTLE] true draws a dim, sparse rail on this thread only.
 * false removes that rail. Rails on other screens stay as they are.
 */
object ThreadChrome {
    const val DATE_RULE_FINE = true
    const val SPROCKET_SUBTLE = true
}
