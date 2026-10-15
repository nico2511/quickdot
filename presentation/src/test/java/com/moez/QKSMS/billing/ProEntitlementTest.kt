package dev.octoshrimpy.quik.billing

import dev.octoshrimpy.quik.util.accentAppliesToConversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProEntitlementTest {

    @Test
    fun failedQueryKeepsTheCachedUnlock() {
        assertEquals(ProEntitlement.KEEP, decideProEntitlement(false, purchased = true, pending = false))
        assertEquals(ProEntitlement.KEEP, decideProEntitlement(false, purchased = false, pending = true))
        assertEquals(ProEntitlement.KEEP, decideProEntitlement(false, purchased = false, pending = false))
    }

    @Test
    fun purchasedUnlocksEvenWhenAnotherRowIsPending() {
        assertEquals(ProEntitlement.GRANT, decideProEntitlement(true, purchased = true, pending = true))
        assertEquals(ProEntitlement.GRANT, decideProEntitlement(true, purchased = true, pending = false))
    }

    @Test
    fun pendingDoesNotUnlock() {
        assertEquals(ProEntitlement.PENDING, decideProEntitlement(true, purchased = false, pending = true))
    }

    @Test
    fun refundOrNoPurchaseLocks() {
        assertEquals(ProEntitlement.REVOKE, decideProEntitlement(true, purchased = false, pending = false))
    }

    @Test
    fun retryDelayDoublesAndCaps() {
        assertEquals(1_000L, proRetryDelayMs(0))
        assertEquals(2_000L, proRetryDelayMs(1))
        assertEquals(4_000L, proRetryDelayMs(2))
        assertEquals(16_000L, proRetryDelayMs(4))
        assertEquals(30_000L, proRetryDelayMs(5))
        assertEquals(30_000L, proRetryDelayMs(12))
    }

    @Test
    fun fossAndUnlockedApplyTheAccentEverywhere() {
        assertTrue(accentAppliesToConversation(false, unlocked = false, styledThreadId = 4L, threadId = 9L))
        assertTrue(accentAppliesToConversation(true, unlocked = true, styledThreadId = 4L, threadId = 9L))
    }

    @Test
    fun lockedPlayAccountStylesOnlyTheChosenConversation() {
        assertTrue(accentAppliesToConversation(true, unlocked = false, styledThreadId = 4L, threadId = 4L))
        assertFalse(accentAppliesToConversation(true, unlocked = false, styledThreadId = 4L, threadId = 9L))
        assertFalse(accentAppliesToConversation(true, unlocked = false, styledThreadId = 4L, threadId = 0L))
        assertFalse(accentAppliesToConversation(true, unlocked = false, styledThreadId = 0L, threadId = 4L))
    }
}
