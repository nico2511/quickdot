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
    val summary: String,
    val purchaseLabel: String,
    val purchaseEnabled: Boolean,
    val actionsVisible: Boolean
)
