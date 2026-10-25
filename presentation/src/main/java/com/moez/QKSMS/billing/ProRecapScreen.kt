package dev.octoshrimpy.quik.billing

/**
 * Pure UI state for the Quickdot Pro recap screen (unit-tested).
 */
data class ProRecapScreenState(
    val statusTitle: String,
    val statusSummary: String,
    val showUnlockedBadge: Boolean,
    val actionsVisible: Boolean,
    val buyLabel: String,
    val buyEnabled: Boolean,
    val restoreVisible: Boolean,
)

data class ProRecapCopy(
    val fossIncludedTitle: String,
    val fossIncludedSummary: String,
    val unlockedTitle: String,
    val unlockedSummary: String,
    val purchaseVerb: String,
    val restore: String,
)

fun buyButtonLabel(purchaseVerb: String, storePrice: String?): String {
    return if (storePrice != null) "$purchaseVerb · $storePrice" else purchaseVerb
}

fun resolveProRecapScreenState(
    enforcementEnabled: Boolean,
    proUnlocked: Boolean,
    offer: ProOfferState,
    copy: ProRecapCopy,
): ProRecapScreenState {
    if (!enforcementEnabled) {
        return ProRecapScreenState(
            statusTitle = copy.fossIncludedTitle,
            statusSummary = copy.fossIncludedSummary,
            showUnlockedBadge = false,
            actionsVisible = false,
            buyLabel = "",
            buyEnabled = false,
            restoreVisible = false,
        )
    }
    if (proUnlocked) {
        return ProRecapScreenState(
            statusTitle = copy.unlockedTitle,
            statusSummary = offer.summary.ifBlank { copy.unlockedSummary },
            showUnlockedBadge = true,
            actionsVisible = false,
            buyLabel = "",
            buyEnabled = false,
            restoreVisible = false,
        )
    }
    return ProRecapScreenState(
        statusTitle = copy.purchaseVerb,
        statusSummary = offer.summary,
        showUnlockedBadge = false,
        actionsVisible = offer.actionsVisible,
        buyLabel = buyButtonLabel(copy.purchaseVerb, offer.storePrice),
        buyEnabled = offer.purchaseEnabled,
        restoreVisible = offer.actionsVisible,
    )
}
