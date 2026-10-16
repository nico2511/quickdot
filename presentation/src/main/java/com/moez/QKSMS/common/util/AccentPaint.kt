package dev.octoshrimpy.quik.common.util

import dev.octoshrimpy.quik.util.Preferences

/**
 * Graphite is a dark ink (#3A3A3A). On a white page that is about 11:1.
 * On the night compose bar (#2A2A2A) it is about 1.3:1, so the send
 * square and the swatch disappear. Night paints a lighter grey that
 * stays at least 3:1 against #1B1B1B and #2A2A2A, and keeps the white
 * icon above 4:1. The saved preference stays #3A3A3A.
 */
object AccentPaint {

    const val GRAPHITE = 0xFF3A3A3A.toInt()
    const val GRAPHITE_ON_NIGHT = 0xFF7A7A7A.toInt()

    fun display(snapped: Int, night: Boolean): Int {
        return if (night && snapped == GRAPHITE) GRAPHITE_ON_NIGHT else snapped
    }

    /**
     * Night graphite is a paint colour, not a seventh accent. Snapping it by
     * RGB distance lands on Plum, because #3A3A3A is farther from #7A7A7A
     * than Plum is. Map it back before that distance check.
     */
    fun canonicalize(color: Int): Int? {
        return if (color == GRAPHITE_ON_NIGHT) GRAPHITE else null
    }

    fun night(mode: Int, scheduledNight: Boolean, systemNight: Boolean): Boolean {
        return when (mode) {
            Preferences.NIGHT_MODE_ON -> true
            Preferences.NIGHT_MODE_OFF -> false
            Preferences.NIGHT_MODE_AUTO -> scheduledNight
            else -> systemNight
        }
    }
}
