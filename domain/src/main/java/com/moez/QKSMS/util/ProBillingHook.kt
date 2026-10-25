package dev.octoshrimpy.quik.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * PLAY BILLING HOOK — Quickdot Pro, 1 EUR, one-time unlock.
 *
 * Do not call [grant] or [revoke] from UI. The Play billing client calls
 * [grant] when Play reports `quickdot_pro` as PURCHASED and [revoke] when a
 * successful query does not (refund, no purchase, or still PENDING).
 * [Preferences.quickdotPro] is the offline cache: a failed or disconnected
 * Play query must not clear it.
 *
 * SMS, MMS, notifications, and sending are never gated. Accent painting is
 * gated via [resolveEffectiveAccentStored] and [resolveEffectiveAccentForThread].
 *
 * The `play` flavor enforces the gate. The `foss` flavor leaves it off, so
 * the accent applies everywhere and no Play Billing classes are on the classpath.
 */
@Singleton
class ProBillingHook @Inject constructor(
    private val prefs: Preferences,
    private val enforcement: StyleEnforcement
) {

    val enforcementEnabled: Boolean get() = enforcement.enabled

    fun isUnlocked(): Boolean = prefs.quickdotPro.get()

    fun grant() {
        prefs.quickdotPro.set(true)
    }

    fun revoke() {
        prefs.quickdotPro.set(false)
    }
}
