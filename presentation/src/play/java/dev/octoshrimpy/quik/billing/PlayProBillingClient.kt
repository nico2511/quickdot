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
import dev.octoshrimpy.quik.BuildConfig
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.util.ProBillingHook
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time product `quickdot_pro`. Cold start and every return to the
 * foreground call [queryPurchasesAsync][BillingClient.queryPurchasesAsync]
 * for in-app purchases. A PURCHASED row unlocks every conversation only
 * after its signature matches [BuildConfig.PLAY_LICENSE_KEY] (SHA1withRSA).
 * A successful query with no trusted PURCHASED row (refund, still PENDING,
 * or a bad signature) locks style to one conversation. A failed or
 * disconnected query keeps the cached unlock and
 * retries with backoff while the app is in the foreground. SMS is not involved.
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
    private var storePrice: String? = null
    private var started = false
    private var connecting = false
    private var inForeground = false
    private var connectAttempt = 0
    private var queryAttempt = 0
    private var announceRestore = false
    private var loggedMissingLicenseKey = false
    private var pendingActivity: WeakReference<Activity>? = null
    private val pendingAcks = mutableSetOf<String>()

    private val reconnect = Runnable {
        connecting = false
        connect()
    }
    private val requery = Runnable {
        if (!ready()) connect() else queryPurchases()
    }

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
                inForeground = true
                connectAttempt = 0
                queryAttempt = 0
                refresh()
            }

            override fun onStop(owner: LifecycleOwner) {
                inForeground = false
                main.removeCallbacks(reconnect)
                main.removeCallbacks(requery)
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
        announceRestore = true
        if (!ready()) {
            toast(R.string.settings_pro_restore_offline)
            publishKeep(unavailable = true)
            connectAttempt = 0
            connect()
            return
        }
        queryPurchases()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) return
        if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            queryPurchases()
            return
        }
        // This callback lists the purchases that just changed, not the full
        // inventory. Revoking from it would lock a paying user. Grant a
        // PURCHASED row, ignore PENDING, then query the full in-app list.
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            val ours = purchases.orEmpty().filter { it.products.contains(PRODUCT_ID) && signatureAccepted(it) }
            val purchased = ours.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            purchased.forEach { acknowledge(it) }
            if (purchased.isNotEmpty()) {
                hook.grant()
                publishOwned()
            }
        }
        queryPurchases()
    }

    private fun refresh() {
        if (!started) return
        if (!ready()) {
            connect()
            return
        }
        queryPurchases()
    }

    private fun connect() {
        if (!started || connecting || ready()) return
        main.removeCallbacks(reconnect)
        connecting = true
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connectAttempt = 0
                    queryProduct()
                    queryPurchases()
                } else {
                    Timber.w("Billing setup %s %s", result.responseCode, result.debugMessage)
                    publishKeep(
                        unavailable = result.responseCode ==
                            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
                    )
                    pendingActivity = null
                    scheduleReconnect()
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = false
                Timber.w("Billing service disconnected")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!started || !inForeground) return
        main.removeCallbacks(reconnect)
        val delay = proRetryDelayMs(connectAttempt)
        connectAttempt += 1
        main.postDelayed(reconnect, delay)
    }

    private fun scheduleRequery() {
        if (!started || !inForeground) return
        main.removeCallbacks(requery)
        val delay = proRetryDelayMs(queryAttempt)
        queryAttempt += 1
        main.postDelayed(requery, delay)
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
                storePrice = price
                state.onNext(state.value!!.copy(price = price, storePrice = price))
            }
            val activity = pendingActivity?.get()
            if (activity != null && !activity.isFinishing) {
                pendingActivity = null
                launch(activity, details)
            }
        }
    }

    private fun queryPurchases() {
        if (!ready()) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            applyQuery(result, purchases)
        }
    }

    private fun applyQuery(
        result: BillingResult,
        purchases: List<Purchase>
    ) {
        val succeeded = result.responseCode == BillingClient.BillingResponseCode.OK
        val ours = purchases.filter { it.products.contains(PRODUCT_ID) && signatureAccepted(it) }
        val purchased = ours.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = ours.any { it.purchaseState == Purchase.PurchaseState.PENDING }
        val announce = announceRestore
        when (decideProEntitlement(succeeded, purchased, pending)) {
            ProEntitlement.GRANT -> {
                queryAttempt = 0
                main.removeCallbacks(requery)
                ours.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach { acknowledge(it) }
                hook.grant()
                publishOwned()
                if (announce) toast(R.string.settings_pro_restored)
                announceRestore = false
            }
            ProEntitlement.PENDING -> {
                queryAttempt = 0
                main.removeCallbacks(requery)
                // Payment has not finished. Do not unlock, including a stale cache.
                hook.revoke()
                publishPending()
                if (announce) toast(R.string.settings_pro_summary_pending)
                announceRestore = false
            }
            ProEntitlement.REVOKE -> {
                queryAttempt = 0
                main.removeCallbacks(requery)
                hook.revoke()
                publishLocked()
                if (announce) toast(R.string.settings_pro_nothing_to_restore)
                announceRestore = false
            }
            ProEntitlement.KEEP -> {
                val unavailable = result.responseCode ==
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
                publishKeep(unavailable)
                if (announce) toast(R.string.settings_pro_restore_offline)
                announceRestore = false
                scheduleRequery()
            }
        }
    }

    /** Unsigned or invalid purchases are ignored, so they cannot unlock. */
    private fun signatureAccepted(purchase: Purchase): Boolean {
        val key = BuildConfig.PLAY_LICENSE_KEY
        if (key.isBlank() && !BuildConfig.DEBUG && !loggedMissingLicenseKey) {
            loggedMissingLicenseKey = true
            Timber.w("PLAY_LICENSE_KEY is empty. This release build will not unlock Pro.")
        }
        val accepted = purchaseMayUnlock(
            purchase.originalJson,
            purchase.signature,
            key,
            BuildConfig.DEBUG
        )
        if (!accepted && key.isNotBlank()) {
            Timber.w("Rejected quickdot_pro. The purchase signature did not verify.")
        }
        return accepted
    }

    private fun acknowledge(purchase: Purchase, attempt: Int = 0) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged || !ready()) return
        if (!pendingAcks.add(purchase.purchaseToken)) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            pendingAcks.remove(purchase.purchaseToken)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) return@acknowledgePurchase
            Timber.w("Acknowledge %s %s", result.responseCode, result.debugMessage)
            if (!inForeground) return@acknowledgePurchase
            main.postDelayed({
                acknowledge(purchase, attempt + 1)
            }, proRetryDelayMs(attempt))
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
                storePrice = storePrice,
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
                storePrice = storePrice,
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
                    storePrice = storePrice,
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
                storePrice = storePrice,
                summary = context.getString(R.string.settings_pro_summary),
                purchaseLabel = context.getString(R.string.settings_pro_purchase),
                purchaseEnabled = true,
                actionsVisible = true
            )
        } else {
            ProOfferState(
                price = price ?: context.getString(R.string.settings_pro_price),
                storePrice = storePrice,
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
