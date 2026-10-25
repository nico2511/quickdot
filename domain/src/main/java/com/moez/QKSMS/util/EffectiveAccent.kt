package dev.octoshrimpy.quik.util

/**
 * Resolves the stored accent preference into the colour that may be painted in the UI.
 *
 * Rule: effective stored accent = (Pro unlocked, or Play enforcement off) ? chosen : Signal.
 * The preference value is never rewritten on refund; only painting changes.
 */
fun resolveEffectiveAccentStored(
    storedSnapped: Int,
    enforcementEnabled: Boolean,
    proUnlocked: Boolean,
): Int {
    val mayPaintChosen = !enforcementEnabled || proUnlocked
    return if (mayPaintChosen) storedSnapped else Preferences.DEFAULT_THEME_COLOR
}
