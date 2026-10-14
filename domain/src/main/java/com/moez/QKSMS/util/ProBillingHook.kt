package dev.octoshrimpy.quik.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * PLAY BILLING HOOK — Quickdot Pro, 1 EUR, one-time unlock.
 *
 * Do not call [grant] or [revoke] from UI. The Play billing client calls
 * [grant] after a purchased `quickdot_pro` is acknowledged and [revoke] when
 * Play reports that the product is not owned (refund, or a fresh install with
 * no purchase). [Preferences.quickdotPro] is the offline cache: a failed or
 * disconnected Play query must not clear it.
 *
 * SMS, MMS, notifications, and sending are never gated. This only decides
 * whether the chosen accent applies to every conversation or to
 * [Preferences.styledThreadId] alone.
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

    /** App chrome always uses the chosen accent. This gate is per conversation. */
    fun accentAppliesTo(threadId: Long): Boolean {
        if (!enforcementEnabled || isUnlocked()) return true
        return threadId != 0L && threadId == prefs.styledThreadId.get()
    }
}
