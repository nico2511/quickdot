package dev.octoshrimpy.quik.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProRecapScreenTest {

    private val copy = ProRecapCopy(
        fossIncludedTitle = "Everything is included",
        fossIncludedSummary = "FOSS summary",
        unlockedTitle = "Unlocked",
        unlockedSummary = "Owned summary",
        purchaseVerb = "Purchase",
        restore = "Restore",
    )

    private fun lockedOffer(storePrice: String? = "€0,99") = ProOfferState(
        price = "1 EUR · one-time",
        storePrice = storePrice,
        summary = "Locked summary",
        purchaseLabel = "Purchase",
        purchaseEnabled = true,
        actionsVisible = true,
    )

    @Test
    fun fossBuildShowsIncludedWithoutActions() {
        val state = resolveProRecapScreenState(
            enforcementEnabled = false,
            proUnlocked = false,
            offer = lockedOffer(),
            copy = copy,
        )
        assertEquals("Everything is included", state.statusTitle)
        assertFalse(state.actionsVisible)
        assertFalse(state.showUnlockedBadge)
    }

    @Test
    fun playFreeShowsBuyWithStorePrice() {
        val state = resolveProRecapScreenState(
            enforcementEnabled = true,
            proUnlocked = false,
            offer = lockedOffer("€1,00"),
            copy = copy,
        )
        assertTrue(state.actionsVisible)
        assertEquals("Purchase · €1,00", state.buyLabel)
        assertTrue(state.buyEnabled)
        assertTrue(state.restoreVisible)
    }

    @Test
    fun playUnlockedShowsBadgeWithoutActions() {
        val state = resolveProRecapScreenState(
            enforcementEnabled = true,
            proUnlocked = true,
            offer = lockedOffer(),
            copy = copy,
        )
        assertTrue(state.showUnlockedBadge)
        assertFalse(state.actionsVisible)
        assertEquals("Unlocked", state.statusTitle)
    }

    @Test
    fun priceUnavailableUsesPurchaseVerbOnly() {
        val label = buyButtonLabel("Purchase", null)
        assertEquals("Purchase", label)
        val state = resolveProRecapScreenState(
            enforcementEnabled = true,
            proUnlocked = false,
            offer = lockedOffer(storePrice = null),
            copy = copy,
        )
        assertEquals("Purchase", state.buyLabel)
    }
}
