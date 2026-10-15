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
        return accentAppliesToConversation(
            enforcementEnabled,
            isUnlocked(),
            prefs.styledThreadId.get(),
            threadId
        )
    }
}

/**
 * Play enforcement off (FOSS) or an unlocked purchase: every conversation.
 * Otherwise only the one saved conversation id.
 */
fun accentAppliesToConversation(
    enforcementEnabled: Boolean,
    unlocked: Boolean,
    styledThreadId: Long,
    threadId: Long
): Boolean {
    if (!enforcementEnabled || unlocked) return true
    return threadId != 0L && threadId == styledThreadId
}
