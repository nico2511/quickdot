package dev.octoshrimpy.quik.billing

import android.app.Activity
import io.reactivex.Observable

/**
 * Play purchase and restore for Quickdot Pro. The FOSS flavor's implementation
 * does not link Play Billing. Neither implementation gates SMS.
 */
interface ProBillingClient {
    val states: Observable<ProOfferState>
    fun start()
    fun purchase(activity: Activity)
    fun restore()
}

data class ProOfferState(
    val price: String,
    /** Formatted one-time price from Play when known; null until product details load. */
    val storePrice: String? = null,
    val summary: String,
    val purchaseLabel: String,
    val purchaseEnabled: Boolean,
    val actionsVisible: Boolean
)
