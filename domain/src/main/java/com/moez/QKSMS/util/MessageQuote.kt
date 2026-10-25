package dev.octoshrimpy.quik.util

/**
 * Whether starting a new reply quote is allowed on this build and Pro state.
 *
 * Play without Pro blocks new quotes; FOSS and unlocked Pro allow them.
 * Existing quoted bodies in the thread are not affected.
 */
fun canStartMessageQuote(enforcementEnabled: Boolean, proUnlocked: Boolean): Boolean {
    return !enforcementEnabled || proUnlocked
}

fun messageQuoteLocked(enforcementEnabled: Boolean, proUnlocked: Boolean): Boolean {
    return enforcementEnabled && !proUnlocked
}
