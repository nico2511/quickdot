package dev.octoshrimpy.quik.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.util.ProBillingHook
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time product `quickdot_pro`. Entitlement is cached in [ProBillingHook]
 * so style stays unlocked when Play cannot be reached. A successful query
 * that does not include the product clears the cache (refund or no purchase).
 * Pending purchases do not unlock style. SMS is not involved.
 */
@Singleton
class PlayProBillingClient @Inject constructor(
    private val context: Context,
    private val hook: ProBillingHook
) : ProBillingClient, PurchasesUpdatedListener {

    private val main = Handler(Looper.getMainLooper())
    private val state = BehaviorSubject.createDefault(offer(locked = !hook.isUnlocked(), price = null))
    override val states: Observable<ProOfferState> = state.hide()

    private lateinit var billingClient: BillingClient
    private var productDetails: ProductDetails? = null
    private var started = false
    private var connecting = false
    private var pendingActivity: WeakReference<Activity>? = null

    override fun start() {
        if (started) {
            refresh()
            return
        }
        started = true
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()
        connect()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                refresh()
            }
        })
    }

    override fun purchase(activity: Activity) {
        if (hook.isUnlocked()) return
        val details = productDetails
        if (ready() && details != null) {
            launch(activity, details)
            return
        }
        pendingActivity = WeakReference(activity)
        if (!started) start() else if (!ready()) connect() else queryProduct()
        // No Play Store on this device: setup often never calls back. Say so now.
        if (!ready()) toast(R.string.settings_pro_summary_unavailable)
    }

    override fun restore() {
        if (!ready()) {
            toast(R.string.settings_pro_restore_offline)
            publishKeep(unavailable = true)
            if (started) connect()
            return
        }
        queryPurchases(userInitiated = true)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) return
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryPurchases(userInitiated = false)
            return
        }
        applyQuery(result, purchases.orEmpty(), userInitiated = false)
    }

    private fun refresh() {
        if (!started) return
        if (!ready()) {
            connect()
            return
        }
        queryPurchases(userInitiated = false)
    }

    private fun connect() {
        if (!started || connecting || ready()) return
        connecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    queryPurchases(userInitiated = false)
                } else {
                    Timber.w("Billing setup %s %s", result.responseCode, result.debugMessage)
                    publishKeep(
                        unavailable = result.responseCode ==
                            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
                    )
                    pendingActivity = null
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                Timber.w("Billing service disconnected")
            }
        })
    }

    private fun queryProduct() {
        if (!ready()) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Timber.w("Product query %s %s", result.responseCode, result.debugMessage)
                if (pendingActivity?.get() != null) {
                    toast(R.string.settings_pro_summary_unavailable)
                    pendingActivity = null
                }
                return@queryProductDetailsAsync
            }
            val details = detailsResult.productDetailsList
                .firstOrNull { it.productId == PRODUCT_ID }
            if (details == null) {
                Timber.w("quickdot_pro is not available. Create and activate it in Play Console.")
                if (pendingActivity?.get() != null) {
                    toast(R.string.settings_pro_product_missing)
                    pendingActivity = null
                }
                return@queryProductDetailsAsync
            }
            productDetails = details
            val price = oneTimeOffer(details)?.formattedPrice
            if (price != null) {
                state.onNext(state.value!!.copy(price = price))
            }
            val activity = pendingActivity?.get()
            if (activity != null && !activity.isFinishing) {
                pendingActivity = null
                launch(activity, details)
            }
        }
    }

    private fun queryPurchases(userInitiated: Boolean) {
        if (!ready()) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            applyQuery(result, purchases, userInitiated)
        }
    }

    private fun applyQuery(
        result: BillingResult,
        purchases: List<Purchase>,
        userInitiated: Boolean
    ) {
        val succeeded = result.responseCode == BillingClient.BillingResponseCode.OK
        val ours = purchases.filter { it.products.contains(PRODUCT_ID) }
        val purchased = ours.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = ours.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        when (decideProEntitlement(succeeded, purchased, pending)) {
            ProEntitlement.GRANT -> {
                ours.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach { acknowledge(it) }
                hook.grant()
                publishOwned()
                if (userInitiated) toast(R.string.settings_pro_restored)
            }
            ProEntitlement.PENDING -> {
                if (hook.isUnlocked()) publishOwned() else publishPending()
                if (userInitiated) toast(R.string.settings_pro_summary_pending)
            }
            ProEntitlement.REVOKE -> {
                hook.revoke()
                publishLocked()
                if (userInitiated) toast(R.string.settings_pro_nothing_to_restore)
            }
            ProEntitlement.KEEP -> {
                val unavailable = result.responseCode ==
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
                publishKeep(unavailable)
                if (userInitiated) toast(R.string.settings_pro_restore_offline)
            }
        }
    }

    private fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged || !ready()) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Timber.w("Acknowledge %s %s", result.responseCode, result.debugMessage)
            }
            hook.grant()
            publishOwned()
        }
    }

    private fun launch(activity: Activity, details: ProductDetails) {
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        val token = oneTimeOffer(details)?.offerToken
        if (!token.isNullOrEmpty()) product.setOfferToken(token)
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(product.build()))
            .build()
        val result = billingClient.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w("launchBillingFlow %s %s", result.responseCode, result.debugMessage)
            val message = if (result.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                R.string.settings_pro_summary_unavailable
            } else {
                R.string.settings_pro_restore_offline
            }
            toast(message)
        }
    }

    private fun oneTimeOffer(details: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? {
        val offers = details.oneTimePurchaseOfferDetailsList
        if (!offers.isNullOrEmpty()) return offers.first()
        return details.oneTimePurchaseOfferDetails
    }

    private fun ready(): Boolean = ::billingClient.isInitialized && billingClient.isReady

    private fun currentPrice(): String {
        return state.value?.price ?: context.getString(R.string.settings_pro_price)
    }

    private fun publishOwned() {
        state.onNext(
            ProOfferState(
                price = currentPrice(),
                summary = context.getString(R.string.settings_pro_summary_owned),
                purchaseLabel = context.getString(R.string.settings_pro_owned),
                purchaseEnabled = false,
                actionsVisible = true
            )
        )
    }

    private fun publishPending() {
        state.onNext(
            ProOfferState(
                price = currentPrice(),
                summary = context.getString(R.string.settings_pro_summary_pending),
                purchaseLabel = context.getString(R.string.settings_pro_pending),
                purchaseEnabled = false,
                actionsVisible = true
            )
        )
    }

    private fun publishLocked() {
        state.onNext(offer(locked = true, price = currentPrice()))
    }

    private fun publishKeep(unavailable: Boolean) {
        when {
            hook.isUnlocked() -> publishOwned()
            unavailable -> state.onNext(
                ProOfferState(
                    price = currentPrice(),
                    summary = context.getString(R.string.settings_pro_summary_unavailable),
                    purchaseLabel = context.getString(R.string.settings_pro_purchase),
                    purchaseEnabled = true,
                    actionsVisible = true
                )
            )
            else -> publishLocked()
        }
    }

    private fun offer(locked: Boolean, price: String?): ProOfferState {
        return if (locked) {
            ProOfferState(
                price = price ?: context.getString(R.string.settings_pro_price),
                summary = context.getString(R.string.settings_pro_summary),
                purchaseLabel = context.getString(R.string.settings_pro_purchase),
                purchaseEnabled = true,
                actionsVisible = true
            )
        } else {
            ProOfferState(
                price = price ?: context.getString(R.string.settings_pro_price),
                summary = context.getString(R.string.settings_pro_summary_owned),
                purchaseLabel = context.getString(R.string.settings_pro_owned),
                purchaseEnabled = false,
                actionsVisible = true
            )
        }
    }

    private fun toast(message: Int) {
        main.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val PRODUCT_ID = "quickdot_pro"
    }
}
