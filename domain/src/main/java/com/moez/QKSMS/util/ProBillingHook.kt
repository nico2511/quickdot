package dev.octoshrimpy.quik.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * PLAY BILLING HOOK — Quickdot Pro, 1 EUR, one-time unlock.
 *
 * Do not call [grant] or [revoke] from UI. The future billing client calls
 * [grant] after a successful purchase and [revoke] if that purchase is refunded.
 * There is no BillingClient in this build.
 *
 * [enforcementEnabled] stays false until that client lands. While it is false,
 * [accentAppliesTo] is true for every thread, so the accent chosen in Settings
 * applies app-wide. When billing ships, set this to true: free users keep the
 * accent on [Preferences.styledThreadId] only, and Pro keeps it everywhere.
 */
@Singleton
class ProBillingHook @Inject constructor(
    private val prefs: Preferences
) {

    val enforcementEnabled: Boolean = false

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
